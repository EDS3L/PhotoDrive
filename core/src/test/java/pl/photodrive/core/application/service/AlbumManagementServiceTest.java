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
import pl.photodrive.core.application.command.album.CreateAlbumCommand;
import pl.photodrive.core.application.command.album.GetPhotoPathCommand;
import pl.photodrive.core.application.command.album.RemoveAlbumCommand;
import pl.photodrive.core.application.command.album.SwapFileCommand;
import pl.photodrive.core.application.exception.SecurityException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        storageField.set(service, Path.of("/tmp/test-storage"));

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
}
