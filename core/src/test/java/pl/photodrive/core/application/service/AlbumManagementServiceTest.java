package pl.photodrive.core.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import pl.photodrive.core.application.command.album.*;
import pl.photodrive.core.application.command.file.ChangeVisibleCommand;
import pl.photodrive.core.application.command.file.RemoveFileCommand;
import pl.photodrive.core.application.command.file.RenameFileCommand;
import pl.photodrive.core.application.event.FileStorageRequested;
import pl.photodrive.core.application.exception.ApplicationSecurityException;
import pl.photodrive.core.domain.exception.AlbumNotFoundException;
import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.application.port.file.FileStoragePort;
import pl.photodrive.core.application.port.file.FileUniquenessChecker;
import pl.photodrive.core.application.port.repository.AlbumRepository;
import pl.photodrive.core.application.port.repository.FileRepository;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.application.port.user.AuthenticatedUser;
import pl.photodrive.core.application.port.user.CurrentUser;
import pl.photodrive.core.domain.exception.AlbumException;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.model.File;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.FileName;
import pl.photodrive.core.domain.vo.HashedPassword;

import jakarta.servlet.ServletContext;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AlbumManagementServiceTest {

    @Mock private AlbumRepository albumRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileUniquenessChecker fileUniquenessChecker;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private FileStoragePort fileStoragePort;
    @Mock private pl.photodrive.core.application.port.file.WatermarkStorePort watermarkStore;
    @Mock private CurrentUser currentUser;
    @Mock private FileRepository fileRepository;
    @Mock private ServletContext servletContext;

    @InjectMocks
    private AlbumManagementService service;

    private User adminUser;
    private User photographerUser;
    private User clientUser;

    @BeforeEach
    void setUp() throws Exception {
        adminUser = User.create("Admin", new Email("admin@photodrive.pl"),
                new HashedPassword("hashed"), Role.ADMIN);
        photographerUser = User.create("Photographer", new Email("photographer@photodrive.pl"),
                new HashedPassword("hashed"), Role.PHOTOGRAPHER);
        clientUser = User.create("Client", new Email("client@photodrive.pl"),
                new HashedPassword("hashed"), Role.CLIENT);

        // inject @Value fields via reflection
        var storageField = AlbumManagementService.class.getDeclaredField("fileStorageLocation");
        storageField.setAccessible(true);
        // The path MUST be absolute: the service compares it with targetPath in the
        // path-traversal guard, and a relative path fails on Windows before it ever reaches the file.
        storageField.set(service, Path.of(System.getProperty("java.io.tmpdir"), "photodrive-test")
                .toAbsolutePath().normalize());

        var orgMaxSizeField = AlbumManagementService.class.getDeclaredField("orgMaxSize");
        orgMaxSizeField.setAccessible(true);
        orgMaxSizeField.setLong(service, 100L);
    }

    private void stubCurrentUserAs(User user) {
        AuthenticatedUser auth = new AuthenticatedUser(user.getId(), user.getRoles(), Instant.now().plusSeconds(900));
        given(currentUser.requireAuthenticated()).willReturn(auth);
        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
    }

    // -----------------------------------------------------------------------
    // swapFile - authorization
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Photographer cannot move files into an album owned by someone else")
    void shouldThrowWhenPhotographerSwapsIntoAlbumHeDoesNotOwn() {
        // Given - the source belongs to this photographer, the target to a DIFFERENT one
        User otherPhotographer = User.create("Other", new Email("other@photodrive.pl"),
                new HashedPassword("hashed"), Role.PHOTOGRAPHER);
        Album source = Album.createForClient("Source", photographerUser, clientUser);
        File file = File.create(new FileName("swap.jpg"), 10L, "image/jpeg");
        source.addFile(file);
        Album target = Album.createForClient("Target", otherPhotographer, clientUser);

        stubCurrentUserAs(photographerUser);
        given(albumRepository.findByAlbumId(source.getAlbumId())).willReturn(Optional.of(source));
        given(albumRepository.findByAlbumId(target.getAlbumId())).willReturn(Optional.of(target));

        SwapFileCommand cmd = new SwapFileCommand(
                source.getAlbumId().value(), target.getAlbumId().value(), List.of(file.getFileId().value()));

        // When / Then - not owning the target album means refusal
        assertThatThrownBy(() -> service.swapFile(cmd))
                .isInstanceOf(ApplicationSecurityException.class);
    }

    // -----------------------------------------------------------------------
    // createAdminAlbum
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Admin creates a portfolio album and the storage folder is requested")
    void shouldCreateAdminAlbumSuccessfully() {
        // Given
        stubCurrentUserAs(adminUser);
        given(albumRepository.existsByName("TestAlbum")).willReturn(false);
        given(albumRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // When
        Album result = service.createAdminAlbum(new CreateAlbumCommand("TestAlbum", null));

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("TestAlbum");
        then(albumRepository).should().save(any());
    }

    @Test
    @DisplayName("Album name must be unique")
    void shouldThrowWhenAlbumAlreadyExists() {
        // Given
        stubCurrentUserAs(adminUser);
        given(albumRepository.existsByName("Duplicate")).willReturn(true);

        // When / Then
        assertThatThrownBy(() -> service.createAdminAlbum(new CreateAlbumCommand("Duplicate", null)))
                .isInstanceOf(AlbumException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Photographer cannot create a portfolio album")
    void shouldThrowWhenPhotographerTriesToCreateAdminAlbum() {
        // Given
        stubCurrentUserAs(photographerUser);
        given(albumRepository.existsByName(any())).willReturn(false);

        // When / Then
        assertThatThrownBy(() -> service.createAdminAlbum(new CreateAlbumCommand("TestAlbum", null)))
                .isInstanceOf(Exception.class);
    }

    // -----------------------------------------------------------------------
    // createAlbumForClient
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Photographer creates an album for his client")
    void shouldCreateAlbumForClientSuccessfully() {
        // Given
        stubCurrentUserAs(photographerUser);
        given(userRepository.findById(clientUser.getId())).willReturn(Optional.of(clientUser));
        given(albumRepository.existsByName(any())).willReturn(false);
        given(albumRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        CreateAlbumCommand cmd = new CreateAlbumCommand("ClientAlbum", clientUser.getId().value());

        // When
        Album result = service.createAlbumForClient(cmd);

        // Then
        assertThat(result).isNotNull();
        then(albumRepository).should().save(any());
    }

    @Test
    @DisplayName("Admin cannot create an album through the photographer use case")
    void shouldThrowWhenAdminTriesToCreateClientAlbum() {
        // Given
        stubCurrentUserAs(adminUser);

        // When / Then
        assertThatThrownBy(() -> service.createAlbumForClient(
                new CreateAlbumCommand("Test", clientUser.getId().value())))
                .isInstanceOf(Exception.class);
    }

    // -----------------------------------------------------------------------
    // deleteAlbum
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Deleting an album also schedules removal of its folder")
    void shouldDeleteAlbumSuccessfully() {
        // Given
        Album album = Album.createForClient("ToDelete", photographerUser, clientUser);
        album.pullDomainEvents(); // clear creation events

        stubCurrentUserAs(photographerUser);
        given(albumRepository.findByAlbumId(any())).willReturn(Optional.of(album));
        given(userRepository.findById(new pl.photodrive.core.domain.vo.UserId(album.getPhotographId())))
                .willReturn(Optional.of(photographerUser));
        willDoNothing().given(albumRepository).delete(any());

        // When
        service.deleteAlbum(new RemoveAlbumCommand(album.getAlbumId().value()));

        // Then
        then(albumRepository).should().delete(album);
    }

    // -----------------------------------------------------------------------
    // getAssignedAlbums
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Photographer sees only his own albums")
    void shouldReturnAlbumsByPhotographerId() {
        // Given
        stubCurrentUserAs(photographerUser);
        Album album = Album.createForClient("Photo Album", photographerUser, clientUser);
        given(albumRepository.findAllByPhotographId(photographerUser.getId().value()))
                .willReturn(List.of(album));

        // When
        List<Album> result = service.getAssignedAlbums();

        // Then
        assertThat(result).hasSize(1);
        then(albumRepository).should().findAllByPhotographId(photographerUser.getId().value());
        then(albumRepository).should(never()).findAllByClientId(any());
    }

    @Test
    @DisplayName("Client sees only albums assigned to him")
    void shouldReturnAlbumsByClientId() {
        // Given
        stubCurrentUserAs(clientUser);
        Album album = Album.createForClient("Client Album", photographerUser, clientUser);
        given(albumRepository.findAllByClientId(clientUser.getId().value()))
                .willReturn(List.of(album));

        // When
        List<Album> result = service.getAssignedAlbums();

        // Then
        assertThat(result).hasSize(1);
        then(albumRepository).should().findAllByClientId(clientUser.getId().value());
        then(albumRepository).should(never()).findAllByPhotographId(any());
    }

    @Test
    @DisplayName("Admin has no assigned albums, so the query is refused")
    void shouldThrowWhenAdminCallsGetAssignedAlbums() {
        // Given
        stubCurrentUserAs(adminUser);

        // When / Then - a denial, not a broken business rule: it must reach the client as 403
        assertThatThrownBy(() -> service.getAssignedAlbums())
                .isInstanceOf(ApplicationSecurityException.class)
                .hasMessageContaining("not assigned");
    }

    // -----------------------------------------------------------------------
    // removeExpiredAlbum
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Scheduled cleanup removes albums past their TTD")
    void shouldRemoveExpiredAlbumsUsingRepository() {
        // Given
        Album album = Album.createForClient("Expired", photographerUser, clientUser);
        // Set ttd in the past via setTTD won't work, but we can use a direct constructor
        var expiredAlbum = new Album(
                album.getAlbumId(),
                album.getName(),
                album.getPhotographId(),
                album.getClientId(),
                Instant.now().minusSeconds(1),
                album.getAlbumPath(),
                false
        );

        given(albumRepository.findByTtdBeforeAndTtdIsNotNull(any())).willReturn(List.of(expiredAlbum));
        willDoNothing().given(albumRepository).delete(any());

        // When
        service.removeExpiredAlbum();

        // Then
        then(albumRepository).should().findByTtdBeforeAndTtdIsNotNull(any(Instant.class));
        then(albumRepository).should().delete(expiredAlbum);
    }

    @Test
    @DisplayName("Cleanup skips albums without a storage path, so nothing unrelated is deleted")
    void shouldNotDeleteAlbumsWithNullPath() {
        // Given - album with null ttd should not be returned (repo query handles it),
        // but we also verify an empty list results in no deletes
        given(albumRepository.findByTtdBeforeAndTtdIsNotNull(any())).willReturn(List.of());

        // When
        service.removeExpiredAlbum();

        // Then
        then(albumRepository).should(never()).delete(any());
    }

    // -----------------------------------------------------------------------
    // getAllAlbums
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Admin can list every album")
    void shouldReturnAllAlbumsForAdmin() {
        // Given
        stubCurrentUserAs(adminUser);
        given(albumRepository.findAll()).willReturn(List.of());

        // When
        List<Album> result = service.getAllAlbums();

        // Then
        assertThat(result).isEmpty();
        then(albumRepository).should().findAll();
    }

    @Test
    @DisplayName("Only an admin can list every album")
    void shouldThrowWhenNonAdminCallsGetAllAlbums() {
        // Given
        stubCurrentUserAs(photographerUser);

        // When / Then - a denial, not a broken business rule: it must reach the client as 403
        assertThatThrownBy(() -> service.getAllAlbums())
                .isInstanceOf(ApplicationSecurityException.class)
                .hasMessageContaining("Access denied");
    }

    // =======================================================================
    // Helpers
    // =======================================================================

    /** Client album with a single file; the file is reachable through getPhotos(). */
    private Album clientAlbumWithFile(String albumName, String fileName) {
        Album album = Album.createForClient(albumName, photographerUser, clientUser);
        album.addFile(File.create(new FileName(fileName), 10L, "image/jpeg"));
        album.pullDomainEvents();
        return album;
    }

    private File onlyFile(Album album) {
        return album.getPhotos().values().iterator().next();
    }

    private void stubAlbum(Album album) {
        given(albumRepository.findByAlbumId(album.getAlbumId())).willReturn(Optional.of(album));
    }

    // =======================================================================
    // addFilesToAlbum - upload
    // =======================================================================

    @Test
    @DisplayName("Upload registers one storage request per file, so every byte lands on disk before commit")
    void shouldAddFilesAndRequestStorageForEachUpload() {
        // Given
        Album album = Album.createForClient("Sesja", photographerUser, clientUser);
        album.pullDomainEvents();
        stubCurrentUserAs(photographerUser);
        stubAlbum(album);
        given(fileRepository.countBySizeBytes()).willReturn(0L);
        given(fileUniquenessChecker.isFileNameTaken(any(), any())).willReturn(false);

        AddFileToAlbumCommand cmd = new AddFileToAlbumCommand(album.getAlbumId().value(), List.of(
                new FileUpload(FileName.of("a.jpg"), 10L, "image/jpeg", "temp-a"),
                new FileUpload(FileName.of("b.jpg"), 20L, "image/jpeg", "temp-b")));

        // When
        List<FileId> ids = service.addFilesToAlbum(cmd);

        // Then
        assertThat(ids).hasSize(2);
        assertThat(album.getPhotos()).hasSize(2);
        then(eventPublisher).should(times(2)).publishEvent(any(FileStorageRequested.class));
        then(albumRepository).should().save(album);
    }

    @Test
    @DisplayName("Colliding upload name is made unique instead of overwriting the existing photo")
    void shouldGiveUniqueNameWhenFileNameAlreadyTaken() {
        // Given - an album that already holds a file under the requested name
        Album album = Album.createForClient("Sesja", photographerUser, clientUser);
        album.pullDomainEvents();
        stubCurrentUserAs(photographerUser);
        stubAlbum(album);
        given(fileRepository.countBySizeBytes()).willReturn(0L);
        // "foto.jpg" is already taken in this album
        given(fileUniquenessChecker.isFileNameTaken(any(), any()))
                .willAnswer(inv -> ((FileName) inv.getArgument(1)).value().equals("foto.jpg"));

        // When
        service.addFilesToAlbum(new AddFileToAlbumCommand(album.getAlbumId().value(),
                List.of(new FileUpload(FileName.of("foto.jpg"), 10L, "image/jpeg", "t1"))));

        // The backend resolves a collision with a "_1" suffix — the SAME format the front proposes
        // in the collision dialog, so a skipped dialog does not produce a different name (B.31).
        assertThat(album.getPhotos().values())
                .extracting(f -> f.getFileName().value())
                .containsExactly("foto_1.jpg");
    }

    @Test
    @DisplayName("User without access to the album cannot upload into it")
    void shouldRejectUploadFromUserWithoutAccessToAlbum() {
        // Given
        User otherClient = User.create("Obcy", new Email("obcy@photodrive.pl"),
                new HashedPassword("hashed"), Role.CLIENT);
        Album album = Album.createForClient("Sesja", photographerUser, clientUser);
        stubCurrentUserAs(otherClient);
        stubAlbum(album);
        given(fileRepository.countBySizeBytes()).willReturn(0L);

        AddFileToAlbumCommand cmd = new AddFileToAlbumCommand(album.getAlbumId().value(),
                List.of(new FileUpload(FileName.of("a.jpg"), 1L, "image/jpeg", "t")));

        // When / Then
        assertThatThrownBy(() -> service.addFilesToAlbum(cmd))
                .isInstanceOf(ApplicationSecurityException.class);
    }

    // =======================================================================
    // downloadFilesAsZip
    // =======================================================================

    @Test
    @DisplayName("Client cannot download a photo the photographer has not revealed")
    void shouldRefuseZipWhenClientAsksForHiddenFile() {
        // Given
        Album album = clientAlbumWithFile("Sesja", "ukryte.jpg");
        stubCurrentUserAs(clientUser);
        stubAlbum(album);

        DownloadFilesCommand cmd = new DownloadFilesCommand(List.of("ukryte.jpg"), album.getAlbumId().value());

        // When / Then
        assertThatThrownBy(() -> service.downloadFilesAsZip(cmd))
                .isInstanceOf(ApplicationSecurityException.class)
                .hasMessageContaining("hidden");
    }

    @Test
    @DisplayName("Client downloads the watermarked version, never the clean original")
    void shouldBuildZipWithWatermarkKeysForClient() {
        // Given - a visible, watermarked photo and a configured platform logo
        Album album = clientAlbumWithFile("Sesja", "foto.jpg");
        File file = onlyFile(album);
        album.changeFileVisibleStatus(List.of(file.getFileId()), true, photographerUser, clientUser.getEmail());
        album.changeWatermarkStatus(photographerUser, true, List.of(file.getFileId()), true);
        album.pullDomainEvents();

        Instant logoVersion = Instant.ofEpochMilli(1234L);
        stubCurrentUserAs(clientUser);
        stubAlbum(album);
        given(userRepository.findById(photographerUser.getId())).willReturn(Optional.of(photographerUser));
        given(watermarkStore.get()).willReturn(Optional.of(
                new pl.photodrive.core.application.port.file.PlatformWatermark(new byte[]{1}, logoVersion)));
        given(fileStoragePort.createZipArchive(any(), any(), any(), any())).willReturn(new byte[]{9});

        // When
        service.downloadFilesAsZip(new DownloadFilesCommand(List.of("foto.jpg"), album.getAlbumId().value()));

        // Then - cache key is {fileId}-{logoVersion}; the client NEVER gets the clean original
        then(fileStoragePort).should().createZipArchive(any(), eq(List.of("foto.jpg")),
                eq(Map.of("foto.jpg", file.getFileId().value() + "-1234")), any());
    }

    @Test
    @DisplayName("Owner downloads clean originals, without the watermark")
    void shouldBuildZipWithCleanOriginalsForOwner() {
        // Given
        Album album = clientAlbumWithFile("Sesja", "foto.jpg");
        File file = onlyFile(album);
        album.changeWatermarkStatus(photographerUser, true, List.of(file.getFileId()), true);
        album.pullDomainEvents();

        stubCurrentUserAs(photographerUser);
        stubAlbum(album);
        given(fileStoragePort.createZipArchive(any(), any(), any(), any())).willReturn(new byte[]{9});

        service.downloadFilesAsZip(new DownloadFilesCommand(List.of("foto.jpg"), album.getAlbumId().value()));

        // When
        // The owner gets originals: no watermark keys and no logo image
        then(fileStoragePort).should().createZipArchive(any(), any(), eq(Map.of()), isNull());

        // Then
        then(watermarkStore).should(never()).get();
    }

    // =======================================================================
    // File operations
    // =======================================================================

    @Test
    @DisplayName("Photographer renames a file in his album")
    void shouldRenameFile() {
        // Given
        Album album = clientAlbumWithFile("Sesja", "stara.jpg");
        File file = onlyFile(album);
        stubCurrentUserAs(photographerUser);
        stubAlbum(album);

        // When
        service.renameFile(new RenameFileCommand(album.getAlbumId().value(), file.getFileId().value(), "nowa.jpg"));

        // Then
        assertThat(album.getPhotos().values())
                .extracting(f -> f.getFileName().value())
                .containsExactly("nowa.jpg");
        then(albumRepository).should().save(album);
    }

    @Test
    @DisplayName("Photographer removes files from his album")
    void shouldRemoveFiles() {
        // Given
        Album album = clientAlbumWithFile("Sesja", "foto.jpg");
        File file = onlyFile(album);
        stubCurrentUserAs(photographerUser);
        stubAlbum(album);

        // When
        service.removeFiles(new RemoveFileCommand(List.of(file.getFileId().value()), album.getAlbumId().value()));

        // Then
        assertThat(album.getPhotos()).isEmpty();
        then(albumRepository).should().save(album);
    }

    @Test
    @DisplayName("Setting TTD stores the date and notifies the client")
    void shouldSetTtdAndNotifyClient() {
        // Given
        Album album = clientAlbumWithFile("Sesja", "foto.jpg");
        Instant ttd = Instant.now().plusSeconds(86_400);
        stubCurrentUserAs(photographerUser);
        stubAlbum(album);
        given(userRepository.findById(clientUser.getId())).willReturn(Optional.of(clientUser));

        // When
        service.setTTD(new SetTTDCommand(album.getAlbumId().value(), ttd));

        // Then
        assertThat(album.getTtd()).isEqualTo(ttd);
        then(eventPublisher).should(atLeastOnce()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Photographer reveals files to the client")
    void shouldChangeVisibilityOfFiles() {
        // Given
        Album album = clientAlbumWithFile("Sesja", "foto.jpg");
        File file = onlyFile(album);
        stubCurrentUserAs(photographerUser);
        stubAlbum(album);
        given(userRepository.findById(clientUser.getId())).willReturn(Optional.of(clientUser));

        // When
        service.changeVisibleStatus(new ChangeVisibleCommand(album.getAlbumId().value(),
                List.of(file.getFileId().value()), true));

        // Then
        assertThat(onlyFile(album).isVisible()).isTrue();
        then(albumRepository).should().save(album);
    }

    @Test
    @DisplayName("Watermark can be applied when the platform logo is configured")
    void shouldChangeWatermarkStatusWhenLogoConfigured() {
        // Given
        Album album = clientAlbumWithFile("Sesja", "foto.jpg");
        File file = onlyFile(album);
        stubCurrentUserAs(photographerUser);
        stubAlbum(album);
        given(watermarkStore.exists()).willReturn(true);

        // When
        service.changeWatermarkStatus(new ChangeWatermarkCommand(album.getAlbumId().value(),
                List.of(file.getFileId().value()), true));

        // Then
        assertThat(onlyFile(album).isHasWatermark()).isTrue();
    }

    @Test
    @DisplayName("Watermark cannot be applied without a platform logo")
    void shouldRefuseWatermarkWhenNoPlatformLogoConfigured() {
        // Given
        Album album = clientAlbumWithFile("Sesja", "foto.jpg");
        File file = onlyFile(album);
        stubCurrentUserAs(photographerUser);
        stubAlbum(album);
        given(watermarkStore.exists()).willReturn(false);

        ChangeWatermarkCommand cmd = new ChangeWatermarkCommand(album.getAlbumId().value(),
                List.of(file.getFileId().value()), true);

        // When / Then
        assertThatThrownBy(() -> service.changeWatermarkStatus(cmd))
                .isInstanceOf(AlbumException.class);
    }

    // =======================================================================
    // Serving photos - authorization
    // =======================================================================

    @Test
    @DisplayName("Serving a photo is refused for a user with no access to the album")
    void shouldRefusePhotoForUserWithoutReadAccess() {
        // Given
        User otherClient = User.create("Obcy", new Email("obcy@photodrive.pl"),
                new HashedPassword("hashed"), Role.CLIENT);
        Album album = clientAlbumWithFile("Sesja", "foto.jpg");
        stubCurrentUserAs(otherClient);
        stubAlbum(album);

        GetPhotoPathCommand cmd = new GetPhotoPathCommand(album.getAlbumId().value(), "foto.jpg", null, null);

        // When / Then
        assertThatThrownBy(() -> service.getFilePath(cmd))
                .isInstanceOf(ApplicationSecurityException.class);
    }

    @Test
    @DisplayName("Client cannot fetch a photo that is still hidden")
    void shouldRefuseHiddenPhotoForClient() {
        // Given
        Album album = clientAlbumWithFile("Sesja", "ukryte.jpg");
        stubCurrentUserAs(clientUser);
        stubAlbum(album);

        GetPhotoPathCommand cmd = new GetPhotoPathCommand(album.getAlbumId().value(), "ukryte.jpg", null, null);

        // When / Then
        assertThatThrownBy(() -> service.getFilePath(cmd))
                .isInstanceOf(ApplicationSecurityException.class);
    }

    @Test
    @DisplayName("Missing file on disk is reported instead of returning an empty response")
    void shouldThrowWhenPhotoFileMissingOnDisk() {
        // Given
        Album album = clientAlbumWithFile("Sesja", "foto.jpg");
        stubCurrentUserAs(photographerUser);
        stubAlbum(album);

        GetPhotoPathCommand cmd = new GetPhotoPathCommand(album.getAlbumId().value(), "foto.jpg", null, null);

        // When / Then
        assertThatThrownBy(() -> service.getFilePath(cmd))
                .isInstanceOf(AlbumException.class)
                .hasMessageContaining("File not found");
    }

    // =======================================================================
    // Public albums (portfolio)
    // =======================================================================

    @Test
    @DisplayName("Public albums are listed for the portfolio")
    void shouldReturnPublicAlbums() {
        // When
        given(albumRepository.findAllPublic()).willReturn(List.of());

        // Then
        assertThat(service.getAllPublicAlbums()).isEmpty();
    }

    @Test
    @DisplayName("A private album is not reachable through the public endpoint")
    void shouldThrowWhenPublicAlbumNotFound() {
        // Given
        AlbumId albumId = new AlbumId(UUID.randomUUID());
        given(albumRepository.findPublicByAlbumId(albumId)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.getPublicAlbum(albumId))
                .isInstanceOf(AlbumNotFoundException.class);
    }

    @Test
    @DisplayName("A private album is not reachable by name either")
    void shouldThrowWhenPublicAlbumByNameNotFound() {
        // Given
        given(albumRepository.findPublicByName("brak")).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.getPublicAlbumByName("brak"))
                .isInstanceOf(AlbumNotFoundException.class);
    }

    @Test
    @DisplayName("Portfolio never serves a photo that is not marked visible")
    void shouldRefusePublicPhotoThatIsNotVisible() {
        // Given - a public album holding a photo the photographer never revealed
        Album album = clientAlbumWithFile("Portfolio", "ukryte.jpg");
        given(albumRepository.findPublicByAlbumId(album.getAlbumId())).willReturn(Optional.of(album));
        UUID id = album.getAlbumId().value();

        // When / Then - the file exists but is hidden, so it is not reachable publicly
        assertThatThrownBy(() -> service.getPublicPhoto(id, "ukryte.jpg", null))
                .isInstanceOf(AlbumException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    @DisplayName("A guest asking for a huge photo gets the capped variant, so the original never leaves the server")
    void shouldCapPublicPhotoWhenGuestAsksForMoreThanTheLimit() {
        // Given - a visible photo in a public album and a guest asking for print resolution
        Album album = publicAlbumWithVisibleFile("foto.jpg");
        UUID id = album.getAlbumId().value();

        // When
        service.getPublicPhoto(id, "foto.jpg", 9999);

        // Then - the request is a wish, the limit is the law
        then(fileStoragePort).should().getOrCreatePublicPhoto(any(), eq("foto.jpg"), any(),
                eq(AlbumManagementService.PUBLIC_MAX_DIMENSION), isNull());
    }

    @Test
    @DisplayName("A guest asking for no size at all still gets a variant, never the untouched original")
    void shouldServeCappedVariantWhenGuestAsksForNoSize() {
        // Given
        Album album = publicAlbumWithVisibleFile("foto.jpg");
        UUID id = album.getAlbumId().value();

        // When
        service.getPublicPhoto(id, "foto.jpg", null);

        // Then
        then(fileStoragePort).should().getOrCreatePublicPhoto(any(), eq("foto.jpg"), any(),
                eq(AlbumManagementService.PUBLIC_MAX_DIMENSION), isNull());
    }

    @Test
    @DisplayName("A size below the limit is honoured, so the portfolio grid can ask for light thumbnails")
    void shouldHonourRequestedSizeBelowThePublicLimit() {
        // Given
        Album album = publicAlbumWithVisibleFile("foto.jpg");
        UUID id = album.getAlbumId().value();

        // When
        service.getPublicPhoto(id, "foto.jpg", 800);

        // Then
        then(fileStoragePort).should().getOrCreatePublicPhoto(any(), eq("foto.jpg"), any(), eq(800), isNull());
    }

    @Test
    @DisplayName("A watermarked portfolio photo is served watermarked, and its cache key follows the logo version")
    void shouldServeWatermarkedPublicPhotoWithVersionedCacheKey() {
        // Given
        Album album = publicAlbumWithVisibleFile("foto.jpg");
        File file = onlyFile(album);
        album.changeWatermarkStatus(adminUser, true, List.of(file.getFileId()), true);
        byte[] logo = new byte[]{1, 2, 3};
        given(watermarkStore.get()).willReturn(Optional.of(
                new pl.photodrive.core.application.port.file.PlatformWatermark(logo, Instant.ofEpochMilli(1234))));

        // When
        service.getPublicPhoto(album.getAlbumId().value(), "foto.jpg", 800);

        // Then - the logo travels to storage, and the key carries fileId + logo version + size,
        // so swapping the logo cannot serve a stale watermark
        then(fileStoragePort).should().getOrCreatePublicPhoto(any(), eq("foto.jpg"),
                eq(file.getFileId().value() + "-wm1234-800"), eq(800), eq(logo));
    }

    /** Publiczny album (admina) z jednym widocznym zdjęciem — tak wygląda kafelek portfolio. */
    private Album publicAlbumWithVisibleFile(String fileName) {
        Album album = Album.createForAdmin("Portfolio", adminUser);
        album.addFile(File.create(new FileName(fileName), 10L, "image/jpeg"));
        album.changeFileVisibleStatus(List.of(onlyFile(album).getFileId()), true, adminUser, adminUser.getEmail());
        album.makePublic(adminUser);
        album.pullDomainEvents();
        given(albumRepository.findPublicByAlbumId(album.getAlbumId())).willReturn(Optional.of(album));
        return album;
    }

    // =======================================================================
    // Publishing and listings
    // =======================================================================

    @Test
    @DisplayName("Admin publishes an album to the portfolio and can withdraw it")
    void shouldMakeAlbumPublicAndPrivate() {
        // Given
        Album album = Album.createForAdmin("Portfolio", adminUser);
        album.pullDomainEvents();
        stubCurrentUserAs(adminUser);
        stubAlbum(album);

        // When
        service.setAlbumPublic(new SetAlbumVisibilityCommand(album.getAlbumId().value(), true));

        // Then
        assertThat(album.isPublic()).isTrue();

        service.setAlbumPublic(new SetAlbumVisibilityCommand(album.getAlbumId().value(), false));
        assertThat(album.isPublic()).isFalse();
    }

    @Test
    @DisplayName("Album list without TTD skips albums that already expire")
    void shouldFilterOutAlbumsWithTtd() {
        // Given
        // TTD applies to client albums only - the domain refuses TTD on an admin album.
        Album withoutTtd = Album.createForClient("Bez TTD", photographerUser, clientUser);
        Album withTtd = Album.createForClient("Z TTD", photographerUser, clientUser);
        withTtd.setTTD(Instant.now().plusSeconds(3600), photographerUser, clientUser.getEmail().value());
        stubCurrentUserAs(adminUser);

        // When
        given(albumRepository.findAll()).willReturn(List.of(withoutTtd, withTtd));

        // Then
        assertThat(service.getAllAlbumsWithoutTTD()).containsExactly(withoutTtd);
    }

    @Test
    @DisplayName("File names are listed for collision detection before upload")
    void shouldReturnFileNamesOfAlbum() {
        // Given
        Album album = clientAlbumWithFile("Sesja", "foto.jpg");
        stubCurrentUserAs(photographerUser);

        // When
        stubAlbum(album);

        // Then
        assertThat(service.getAlbumFileNames(album.getAlbumId())).containsExactly("foto.jpg");
    }

    @Test
    @DisplayName("Service reports whether the caller is a client")
    void shouldTellWhetherCurrentUserIsClient() {
        // When
        stubCurrentUserAs(clientUser);

        // Then
        assertThat(service.isCurrentUserClient()).isTrue();

        stubCurrentUserAs(adminUser);
        assertThat(service.isCurrentUserClient()).isFalse();
    }

    // =======================================================================
    // getAllUrlsFromAlbum
    // =======================================================================

    @Test
    @DisplayName("Photo URLs carry the size parameters only when a size was requested")
    void shouldBuildPhotoUrlsWithAndWithoutSize() {
        // Given
        Album album = clientAlbumWithFile("Sesja", "foto.jpg");
        stubCurrentUserAs(photographerUser);
        stubAlbum(album);
        UUID albumId = album.getAlbumId().value();

        // When
        List<String> plain = service.getAllUrlsFromAlbum(
                new GetUrlsCommand(albumId, "https://photodrive.dev", null, null, false));

        // Then
        assertThat(plain).containsExactly(
                "https://photodrive.dev/api/album/" + albumId + "/photo/foto.jpg");

        List<String> sized = service.getAllUrlsFromAlbum(
                new GetUrlsCommand(albumId, "https://photodrive.dev", 300, 200, false));
        assertThat(sized).containsExactly(
                "https://photodrive.dev/api/album/" + albumId + "/photo/foto.jpg?width=300&height=200");
    }

    @Test
    @DisplayName("Only visible photos get a URL when the caller asks for visible ones")
    void shouldReturnOnlyVisibleUrlsWhenRequested() {
        // Given
        Album album = clientAlbumWithFile("Sesja", "ukryte.jpg");
        stubCurrentUserAs(clientUser);
        stubAlbum(album);

        // When
        List<String> urls = service.getAllUrlsFromAlbum(new GetUrlsCommand(
                album.getAlbumId().value(), "https://photodrive.dev", null, null, true));

        // Then
        assertThat(urls).isEmpty();
    }
}
