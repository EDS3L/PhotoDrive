package pl.photodrive.core.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pl.photodrive.core.application.command.album.CreateAlbumCommand;
import pl.photodrive.core.application.port.repository.AlbumRepository;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.application.port.user.CurrentUser;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.Password;
import pl.photodrive.core.domain.vo.UserId;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlbumManagementServiceTest {

    @Mock private AlbumRepository albumRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUser currentUser;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AlbumManagementService albumService;

    @Test
    void shouldCreateAdminAlbumSuccessfully() {
        UserId adminId = UserId.newId();
        User admin = User.create("Admin", new Email("admin@test.pl"), new Password("SecurePass123!"), Role.ADMIN, "raw");

        var auth = mock(pl.photodrive.core.application.port.user.AuthenticatedUser.class);
        when(currentUser.requireAuthenticated()).thenReturn(auth);
        when(auth.userId()).thenReturn(adminId);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(albumRepository.existsByName("Summer")).thenReturn(false);
        when(albumRepository.save(any(Album.class))).thenAnswer(i -> i.getArguments()[0]);

        Album album = albumService.createAdminAlbum(new CreateAlbumCommand("Summer", null));

        assertNotNull(album);
        assertEquals("Summer", album.getName());
        verify(albumRepository).save(any(Album.class));
    }
}
