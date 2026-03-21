package pl.photodrive.core.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.photodrive.core.application.command.auth.LoginCommand;
import pl.photodrive.core.application.dto.AccessToken;
import pl.photodrive.core.application.port.password.PasswordHasher;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.application.port.token.TokenEncoder;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.Password;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthManagerServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private TokenEncoder tokenEncoder;
    @Mock private Clock clock;

    @InjectMocks
    private AuthManagerService authService;

    @Test
    void shouldLoginSuccessfully() {
        LoginCommand cmd = new LoginCommand("test@photodrive.pl", "rawPass");
        User user = User.create("Test", new Email("test@photodrive.pl"), new Password("Hashed123!"), Role.PHOTOGRAPHER, "raw");
        user.setChangePasswordOnNextLogin(false);

        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordHasher.matches(anyString(), anyString())).thenReturn(true);
        when(clock.instant()).thenReturn(Instant.now());
        when(tokenEncoder.createAccessToken(any(), any(), any(), any())).thenReturn("jwt-token");

        AccessToken token = authService.login(cmd);

        assertNotNull(token);
        assertEquals("jwt-token", token.value());
    }
}
