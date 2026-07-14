package pl.photodrive.core.application.service;

import org.junit.jupiter.api.BeforeEach;
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
import pl.photodrive.core.application.exception.SecurityException;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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
        // Ścieżka MUSI być absolutna — serwis porównuje ją z targetPath w guardzie
        // path-traversal; ścieżka względna wywala się na Windowsie zanim dojdzie do pliku.
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
    // swapFile — autoryzacja (B.13)
    // -----------------------------------------------------------------------

    @Test
    void shouldThrowWhenPhotographerSwapsIntoAlbumHeDoesNotOwn() {
        // Given — źródło należy do fotografa, cel do INNEGO fotografa
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

        // When / Then — brak własności albumu docelowego = odmowa (403)
        assertThatThrownBy(() -> service.swapFile(cmd))
                .isInstanceOf(SecurityException.class);
    }

    // -----------------------------------------------------------------------
    // createAdminAlbum
    // -----------------------------------------------------------------------

    @Test
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
        verify(albumRepository).save(any());
    }

    @Test
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
        verify(albumRepository).save(any());
    }

    @Test
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
    void shouldDeleteAlbumSuccessfully() {
        // Given
        Album album = Album.createForClient("ToDelete", photographerUser, clientUser);
        album.pullDomainEvents(); // clear creation events

        stubCurrentUserAs(photographerUser);
        given(albumRepository.findByAlbumId(any())).willReturn(Optional.of(album));
        given(userRepository.findById(new pl.photodrive.core.domain.vo.UserId(album.getPhotographId())))
                .willReturn(Optional.of(photographerUser));
        doNothing().when(albumRepository).delete(any());

        // When
        service.deleteAlbum(new RemoveAlbumCommand(album.getAlbumId().value()));

        // Then
        verify(albumRepository).delete(album);
    }

    // -----------------------------------------------------------------------
    // getAssignedAlbums
    // -----------------------------------------------------------------------

    @Test
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
        verify(albumRepository).findAllByPhotographId(photographerUser.getId().value());
        verify(albumRepository, never()).findAllByClientId(any());
    }

    @Test
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
        verify(albumRepository).findAllByClientId(clientUser.getId().value());
        verify(albumRepository, never()).findAllByPhotographId(any());
    }

    @Test
    void shouldThrowWhenAdminCallsGetAssignedAlbums() {
        // Given
        stubCurrentUserAs(adminUser);

        // When / Then
        assertThatThrownBy(() -> service.getAssignedAlbums())
                .isInstanceOf(AlbumException.class)
                .hasMessageContaining("not assigned");
    }

    // -----------------------------------------------------------------------
    // removeExpiredAlbum
    // -----------------------------------------------------------------------

    @Test
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
        doNothing().when(albumRepository).delete(any());

        // When
        service.removeExpiredAlbum();

        // Then
        verify(albumRepository).findByTtdBeforeAndTtdIsNotNull(any(Instant.class));
        verify(albumRepository).delete(expiredAlbum);
    }

    @Test
    void shouldNotDeleteAlbumsWithNullPath() {
        // Given — album with null ttd should not be returned (repo query handles it),
        // but we also verify an empty list results in no deletes
        given(albumRepository.findByTtdBeforeAndTtdIsNotNull(any())).willReturn(List.of());

        // When
        service.removeExpiredAlbum();

        // Then
        verify(albumRepository, never()).delete(any());
    }

    // -----------------------------------------------------------------------
    // getAllAlbums
    // -----------------------------------------------------------------------

    @Test
    void shouldReturnAllAlbumsForAdmin() {
        // Given
        stubCurrentUserAs(adminUser);
        given(albumRepository.findAll()).willReturn(List.of());

        // When
        List<Album> result = service.getAllAlbums();

        // Then
        assertThat(result).isEmpty();
        verify(albumRepository).findAll();
    }

    @Test
    void shouldThrowWhenNonAdminCallsGetAllAlbums() {
        // Given
        stubCurrentUserAs(photographerUser);

        // When / Then
        assertThatThrownBy(() -> service.getAllAlbums())
                .isInstanceOf(AlbumException.class)
                .hasMessageContaining("Access denied");
    }

    // =======================================================================
    // Pomocnicze
    // =======================================================================

    /** Album klienta z jednym plikiem; zwraca album, plik dostępny przez getPhotos(). */
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
    // addFilesToAlbum — upload
    // =======================================================================

    @Test
    void shouldAddFilesAndRequestStorageForEachUpload() {
        Album album = Album.createForClient("Sesja", photographerUser, clientUser);
        album.pullDomainEvents();
        stubCurrentUserAs(photographerUser);
        stubAlbum(album);
        given(fileRepository.countBySizeBytes()).willReturn(0L);
        given(fileUniquenessChecker.isFileNameTaken(any(), any())).willReturn(false);

        AddFileToAlbumCommand cmd = new AddFileToAlbumCommand(album.getAlbumId().value(), List.of(
                new FileUpload(FileName.of("a.jpg"), 10L, "image/jpeg", "temp-a"),
                new FileUpload(FileName.of("b.jpg"), 20L, "image/jpeg", "temp-b")));

        List<FileId> ids = service.addFilesToAlbum(cmd);

        assertThat(ids).hasSize(2);
        assertThat(album.getPhotos()).hasSize(2);
        verify(eventPublisher, times(2)).publishEvent(any(FileStorageRequested.class));
        verify(albumRepository).save(album);
    }

    @Test
    void shouldGiveUniqueNameWhenFileNameAlreadyTaken() {
        Album album = Album.createForClient("Sesja", photographerUser, clientUser);
        album.pullDomainEvents();
        stubCurrentUserAs(photographerUser);
        stubAlbum(album);
        given(fileRepository.countBySizeBytes()).willReturn(0L);
        // „foto.jpg" jest zajęte, „foto_1.jpg" już nie
        given(fileUniquenessChecker.isFileNameTaken(any(), any()))
                .willAnswer(inv -> ((FileName) inv.getArgument(1)).value().equals("foto.jpg"));

        service.addFilesToAlbum(new AddFileToAlbumCommand(album.getAlbumId().value(),
                List.of(new FileUpload(FileName.of("foto.jpg"), 10L, "image/jpeg", "t1"))));

        // Backend rozwiązuje kolizję sufiksem „ (1)" — NIE „_1" (patrz FileNamingPolicy).
        assertThat(album.getPhotos().values())
                .extracting(f -> f.getFileName().value())
                .containsExactly("foto (1).jpg");
    }

    @Test
    void shouldRejectUploadFromUserWithoutAccessToAlbum() {
        User otherClient = User.create("Obcy", new Email("obcy@photodrive.pl"),
                new HashedPassword("hashed"), Role.CLIENT);
        Album album = Album.createForClient("Sesja", photographerUser, clientUser);
        stubCurrentUserAs(otherClient);
        stubAlbum(album);
        given(fileRepository.countBySizeBytes()).willReturn(0L);

        AddFileToAlbumCommand cmd = new AddFileToAlbumCommand(album.getAlbumId().value(),
                List.of(new FileUpload(FileName.of("a.jpg"), 1L, "image/jpeg", "t")));

        assertThatThrownBy(() -> service.addFilesToAlbum(cmd))
                .isInstanceOf(SecurityException.class);
    }

    // =======================================================================
    // downloadFilesAsZip
    // =======================================================================

    @Test
    void shouldRefuseZipWhenClientAsksForHiddenFile() {
        Album album = clientAlbumWithFile("Sesja", "ukryte.jpg");
        stubCurrentUserAs(clientUser);
        stubAlbum(album);

        DownloadFilesCommand cmd = new DownloadFilesCommand(List.of("ukryte.jpg"), album.getAlbumId().value());

        assertThatThrownBy(() -> service.downloadFilesAsZip(cmd))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("hidden");
    }

    @Test
    void shouldBuildZipWithWatermarkKeysForClient() {
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

        service.downloadFilesAsZip(new DownloadFilesCommand(List.of("foto.jpg"), album.getAlbumId().value()));

        // Klucz cache = {fileId}-{wersjaLoga}; klient NIGDY nie dostaje czystego oryginału
        verify(fileStoragePort).createZipArchive(any(), eq(List.of("foto.jpg")),
                eq(Map.of("foto.jpg", file.getFileId().value() + "-1234")), any());
    }

    @Test
    void shouldBuildZipWithCleanOriginalsForOwner() {
        Album album = clientAlbumWithFile("Sesja", "foto.jpg");
        File file = onlyFile(album);
        album.changeWatermarkStatus(photographerUser, true, List.of(file.getFileId()), true);
        album.pullDomainEvents();

        stubCurrentUserAs(photographerUser);
        stubAlbum(album);
        given(fileStoragePort.createZipArchive(any(), any(), any(), any())).willReturn(new byte[]{9});

        service.downloadFilesAsZip(new DownloadFilesCommand(List.of("foto.jpg"), album.getAlbumId().value()));

        // Właściciel dostaje oryginały — bez kluczy watermarku i bez obrazu loga
        verify(fileStoragePort).createZipArchive(any(), any(), eq(Map.of()), isNull());
        verify(watermarkStore, never()).get();
    }

    // =======================================================================
    // Operacje na plikach
    // =======================================================================

    @Test
    void shouldRenameFile() {
        Album album = clientAlbumWithFile("Sesja", "stara.jpg");
        File file = onlyFile(album);
        stubCurrentUserAs(photographerUser);
        stubAlbum(album);

        service.renameFile(new RenameFileCommand(album.getAlbumId().value(), file.getFileId().value(), "nowa.jpg"));

        assertThat(album.getPhotos().values())
                .extracting(f -> f.getFileName().value())
                .containsExactly("nowa.jpg");
        verify(albumRepository).save(album);
    }

    @Test
    void shouldRemoveFiles() {
        Album album = clientAlbumWithFile("Sesja", "foto.jpg");
        File file = onlyFile(album);
        stubCurrentUserAs(photographerUser);
        stubAlbum(album);

        service.removeFiles(new RemoveFileCommand(List.of(file.getFileId().value()), album.getAlbumId().value()));

        assertThat(album.getPhotos()).isEmpty();
        verify(albumRepository).save(album);
    }

    @Test
    void shouldSetTtdAndNotifyClient() {
        Album album = clientAlbumWithFile("Sesja", "foto.jpg");
        Instant ttd = Instant.now().plusSeconds(86_400);
        stubCurrentUserAs(photographerUser);
        stubAlbum(album);
        given(userRepository.findById(clientUser.getId())).willReturn(Optional.of(clientUser));

        service.setTTD(new SetTTDCommand(album.getAlbumId().value(), ttd));

        assertThat(album.getTtd()).isEqualTo(ttd);
        verify(eventPublisher, atLeastOnce()).publishEvent(any(Object.class));
    }

    @Test
    void shouldChangeVisibilityOfFiles() {
        Album album = clientAlbumWithFile("Sesja", "foto.jpg");
        File file = onlyFile(album);
        stubCurrentUserAs(photographerUser);
        stubAlbum(album);
        given(userRepository.findById(clientUser.getId())).willReturn(Optional.of(clientUser));

        service.changeVisibleStatus(new ChangeVisibleCommand(album.getAlbumId().value(),
                List.of(file.getFileId().value()), true));

        assertThat(onlyFile(album).isVisible()).isTrue();
        verify(albumRepository).save(album);
    }

    @Test
    void shouldChangeWatermarkStatusWhenLogoConfigured() {
        Album album = clientAlbumWithFile("Sesja", "foto.jpg");
        File file = onlyFile(album);
        stubCurrentUserAs(photographerUser);
        stubAlbum(album);
        given(watermarkStore.exists()).willReturn(true);

        service.changeWatermarkStatus(new ChangeWatermarkCommand(album.getAlbumId().value(),
                List.of(file.getFileId().value()), true));

        assertThat(onlyFile(album).isHasWatermark()).isTrue();
    }

    @Test
    void shouldRefuseWatermarkWhenNoPlatformLogoConfigured() {
        Album album = clientAlbumWithFile("Sesja", "foto.jpg");
        File file = onlyFile(album);
        stubCurrentUserAs(photographerUser);
        stubAlbum(album);
        given(watermarkStore.exists()).willReturn(false);

        ChangeWatermarkCommand cmd = new ChangeWatermarkCommand(album.getAlbumId().value(),
                List.of(file.getFileId().value()), true);

        assertThatThrownBy(() -> service.changeWatermarkStatus(cmd))
                .isInstanceOf(AlbumException.class);
    }

    // =======================================================================
    // Serwowanie zdjęć — autoryzacja
    // =======================================================================

    @Test
    void shouldRefusePhotoForUserWithoutReadAccess() {
        User otherClient = User.create("Obcy", new Email("obcy@photodrive.pl"),
                new HashedPassword("hashed"), Role.CLIENT);
        Album album = clientAlbumWithFile("Sesja", "foto.jpg");
        stubCurrentUserAs(otherClient);
        stubAlbum(album);

        GetPhotoPathCommand cmd = new GetPhotoPathCommand(album.getAlbumId().value(), "foto.jpg", null, null);

        assertThatThrownBy(() -> service.getFilePath(cmd))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void shouldRefuseHiddenPhotoForClient() {
        Album album = clientAlbumWithFile("Sesja", "ukryte.jpg");
        stubCurrentUserAs(clientUser);
        stubAlbum(album);

        GetPhotoPathCommand cmd = new GetPhotoPathCommand(album.getAlbumId().value(), "ukryte.jpg", null, null);

        assertThatThrownBy(() -> service.getFilePath(cmd))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void shouldThrowWhenPhotoFileMissingOnDisk() {
        Album album = clientAlbumWithFile("Sesja", "foto.jpg");
        stubCurrentUserAs(photographerUser);
        stubAlbum(album);

        GetPhotoPathCommand cmd = new GetPhotoPathCommand(album.getAlbumId().value(), "foto.jpg", null, null);

        assertThatThrownBy(() -> service.getFilePath(cmd))
                .isInstanceOf(AlbumException.class)
                .hasMessageContaining("File not found");
    }

    // =======================================================================
    // Albumy publiczne (portfolio)
    // =======================================================================

    @Test
    void shouldReturnPublicAlbums() {
        given(albumRepository.findAllPublic()).willReturn(List.of());

        assertThat(service.getAllPublicAlbums()).isEmpty();
    }

    @Test
    void shouldThrowWhenPublicAlbumNotFound() {
        AlbumId albumId = new AlbumId(UUID.randomUUID());
        given(albumRepository.findPublicByAlbumId(albumId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublicAlbum(albumId))
                .isInstanceOf(AlbumNotFoundException.class);
    }

    @Test
    void shouldThrowWhenPublicAlbumByNameNotFound() {
        given(albumRepository.findPublicByName("brak")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublicAlbumByName("brak"))
                .isInstanceOf(AlbumNotFoundException.class);
    }

    @Test
    void shouldRefusePublicPhotoThatIsNotVisible() {
        Album album = clientAlbumWithFile("Portfolio", "ukryte.jpg");
        given(albumRepository.findPublicByAlbumId(album.getAlbumId())).willReturn(Optional.of(album));

        UUID id = album.getAlbumId().value();

        // Plik istnieje, ale jest niewidoczny → publicznie niedostępny
        assertThatThrownBy(() -> service.getPublicPhoto(id, "ukryte.jpg"))
                .isInstanceOf(AlbumException.class)
                .hasMessageContaining("File not found");
    }

    // =======================================================================
    // Publikacja i listy
    // =======================================================================

    @Test
    void shouldMakeAlbumPublicAndPrivate() {
        Album album = Album.createForAdmin("Portfolio", adminUser);
        album.pullDomainEvents();
        stubCurrentUserAs(adminUser);
        stubAlbum(album);

        service.setAlbumPublic(new SetAlbumVisibilityCommand(album.getAlbumId().value(), true));
        assertThat(album.isPublic()).isTrue();

        service.setAlbumPublic(new SetAlbumVisibilityCommand(album.getAlbumId().value(), false));
        assertThat(album.isPublic()).isFalse();
    }

    @Test
    void shouldFilterOutAlbumsWithTtd() {
        // TTD dotyczy wyłącznie albumów klienta — domena odrzuca TTD na albumie admina.
        Album withoutTtd = Album.createForClient("Bez TTD", photographerUser, clientUser);
        Album withTtd = Album.createForClient("Z TTD", photographerUser, clientUser);
        withTtd.setTTD(Instant.now().plusSeconds(3600), photographerUser, clientUser.getEmail().value());
        stubCurrentUserAs(adminUser);
        given(albumRepository.findAll()).willReturn(List.of(withoutTtd, withTtd));

        assertThat(service.getAllAlbumsWithoutTTD()).containsExactly(withoutTtd);
    }

    @Test
    void shouldReturnFileNamesOfAlbum() {
        Album album = clientAlbumWithFile("Sesja", "foto.jpg");
        stubCurrentUserAs(photographerUser);
        stubAlbum(album);

        assertThat(service.getAlbumFileNames(album.getAlbumId())).containsExactly("foto.jpg");
    }

    @Test
    void shouldTellWhetherCurrentUserIsClient() {
        stubCurrentUserAs(clientUser);
        assertThat(service.isCurrentUserClient()).isTrue();

        stubCurrentUserAs(adminUser);
        assertThat(service.isCurrentUserClient()).isFalse();
    }

    // =======================================================================
    // getAllUrlsFromAlbum
    // =======================================================================

    @Test
    void shouldBuildPhotoUrlsWithAndWithoutSize() {
        Album album = clientAlbumWithFile("Sesja", "foto.jpg");
        stubCurrentUserAs(photographerUser);
        stubAlbum(album);
        UUID albumId = album.getAlbumId().value();

        List<String> plain = service.getAllUrlsFromAlbum(
                new GetUrlsCommand(albumId, "https://photodrive.dev", null, null, false));
        assertThat(plain).containsExactly(
                "https://photodrive.dev/api/album/" + albumId + "/photo/foto.jpg");

        List<String> sized = service.getAllUrlsFromAlbum(
                new GetUrlsCommand(albumId, "https://photodrive.dev", 300, 200, false));
        assertThat(sized).containsExactly(
                "https://photodrive.dev/api/album/" + albumId + "/photo/foto.jpg?width=300&height=200");
    }

    @Test
    void shouldReturnOnlyVisibleUrlsWhenRequested() {
        Album album = clientAlbumWithFile("Sesja", "ukryte.jpg");
        stubCurrentUserAs(clientUser);
        stubAlbum(album);

        List<String> urls = service.getAllUrlsFromAlbum(new GetUrlsCommand(
                album.getAlbumId().value(), "https://photodrive.dev", null, null, true));

        assertThat(urls).isEmpty();
    }
}
