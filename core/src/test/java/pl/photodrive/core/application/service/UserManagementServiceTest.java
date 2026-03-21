package pl.photodrive.core.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pl.photodrive.core.application.command.user.AddUserCommand;
import pl.photodrive.core.application.port.password.PasswordHasher;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.application.port.user.CurrentUser;
import pl.photodrive.core.application.port.user.UserUniquenessChecker;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.vo.Email;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private UserUniquenessChecker uniquenessChecker;
    @Mock private CurrentUser currentUser;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserManagementService userService;

    @Test
    void shouldAddUserByAdminSuccessfully() {
        AddUserCommand cmd = new AddUserCommand("NewUser", "new@test.pl", "Pass123!@#", Role.CLIENT);
        
        var auth = mock(pl.photodrive.core.application.port.user.AuthenticatedUser.class);
        when(currentUser.requireAuthenticated()).thenReturn(auth);
        when(auth.roles()).thenReturn(Set.of(Role.ADMIN));
        when(uniquenessChecker.isEmailTaken(any(Email.class))).thenReturn(false);
        when(passwordHasher.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User result = userService.addUser(cmd);

        assertNotNull(result);
        assertEquals("NewUser", result.getName());
        verify(userRepository).save(any(User.class));
    }
}
