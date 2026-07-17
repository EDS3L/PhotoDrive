package pl.photodrive.core.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pl.photodrive.core.application.command.auth.LoginCommand;
import pl.photodrive.core.application.command.auth.RemindPasswordCommand;
import pl.photodrive.core.application.dto.AccessToken;
import pl.photodrive.core.application.exception.LoginFailedException;
import pl.photodrive.core.application.port.repository.PasswordTokenRepository;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.application.port.token.TokenEncoder;
import pl.photodrive.core.domain.exception.PasswordTokenException;
import pl.photodrive.core.domain.model.PasswordToken;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.service.PasswordHasher;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.HashedPassword;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthManagerServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenEncoder tokenEncoder;

    @Mock
    private PasswordTokenRepository passwordTokenRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthManagerService service;

    private final Clock fixedClock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
    private User photographer;

    @BeforeEach
    void injectClock() throws Exception {
        // inject clock via reflection since Lombok @RequiredArgsConstructor wires it by type
        var field = AuthManagerService.class.getDeclaredField("clock");
        field.setAccessible(true);
        field.set(service, fixedClock);

        var ttlField = AuthManagerService.class.getDeclaredField("accessTtlMinutes");
        ttlField.setAccessible(true);
        ttlField.setLong(service, 60L);

        photographer = User.create("Photo", new Email("photo@photodrive.pl"),
                new HashedPassword("hashed_Pass1!"), Role.PHOTOGRAPHER);
        photographer.setChangePasswordOnNextLogin(false);
    }

    // -----------------------------------------------------------------------
    // login
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Successful login returns an access token")
    void shouldReturnAccessTokenOnSuccessfulLogin() {
        // Given
        given(userRepository.findByEmail(any())).willReturn(Optional.of(photographer));
        given(passwordHasher.matches(any(), any())).willReturn(true);
        given(tokenEncoder.createAccessToken(any(), any(), any(), any(), anyBoolean())).willReturn("jwt.token.here");

        LoginCommand cmd = new LoginCommand("photo@photodrive.pl", "Pass1!");

        // When
        AccessToken token = service.login(cmd);

        // Then
        assertThat(token.value()).isEqualTo("jwt.token.here");
    }

    @Test
    @DisplayName("Login fails for an unknown account")
    void shouldThrowWhenUserNotFound() {
        // Given
        given(userRepository.findByEmail(any())).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.login(new LoginCommand("unknown@photodrive.pl", "Pass1!")))
                .isInstanceOf(LoginFailedException.class);
    }

    @Test
    @DisplayName("Deactivated account cannot log in")
    void shouldThrowWhenUserIsInactive() {
        // Given
        User inactiveAdmin = User.create("Admin", new Email("admin@photodrive.pl"),
                new HashedPassword("h"), Role.ADMIN);
        User inactiveUser = User.create("Inactive", new Email("inactive@photodrive.pl"),
                new HashedPassword("h"), Role.PHOTOGRAPHER);
        inactiveUser.deactivateUser(false, inactiveAdmin);
        given(userRepository.findByEmail(any())).willReturn(Optional.of(inactiveUser));

        // When / Then
        assertThatThrownBy(() -> service.login(new LoginCommand("inactive@photodrive.pl", "Pass1!")))
                .isInstanceOf(LoginFailedException.class);
    }

    @Test
    @DisplayName("Login fails on a wrong password")
    void shouldThrowWhenPasswordDoesNotMatch() {
        // Given
        given(userRepository.findByEmail(any())).willReturn(Optional.of(photographer));
        given(passwordHasher.matches(any(), any())).willReturn(false);

        // When / Then
        assertThatThrownBy(() -> service.login(new LoginCommand("photo@photodrive.pl", "WrongPass!")))
                .isInstanceOf(LoginFailedException.class);
    }

    @Test
    @DisplayName("First login with the generated password succeeds and stamps the forced-change flag into the token, so the server can lock the API until the password is changed (B.20)")
    void shouldReturnAccessTokenEvenWhenChangePasswordFlagIsSet() {
        // Given - a user with the "change password" flag MUST still be able to log in with the
        // generated password; the forced change is then enforced server-side via a token flag (B.20).
        photographer.setChangePasswordOnNextLogin(true);
        given(userRepository.findByEmail(any())).willReturn(Optional.of(photographer));
        given(passwordHasher.matches(any(), any())).willReturn(true);
        given(tokenEncoder.createAccessToken(any(), any(), any(), any(), anyBoolean())).willReturn("jwt.token.here");

        // When
        AccessToken token = service.login(new LoginCommand("photo@photodrive.pl", "Pass1!"));

        // Then - login succeeds AND the token is minted with mustChangePassword=true
        assertThat(token.value()).isEqualTo("jwt.token.here");
        then(tokenEncoder).should().createAccessToken(any(), any(), any(), any(), eq(true));
    }

    // -----------------------------------------------------------------------
    // remindPassword
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Password is reset with a valid authorization code")
    void shouldChangePasswordWithValidToken() {
        // Given
        UUID tokenUUID = UUID.randomUUID();
        PasswordToken passwordToken = PasswordToken.create(
                tokenUUID,
                Instant.now().plusSeconds(900),
                Instant.now(),
                photographer);
        passwordToken.pullDomainEvents(); // clear init event

        given(userRepository.findByEmail(any())).willReturn(Optional.of(photographer));
        given(passwordTokenRepository.findByUserId(any())).willReturn(Optional.of(passwordToken));
        given(passwordHasher.matches(any(), any())).willReturn(false);
        given(passwordHasher.encode(any())).willReturn("hashed_NewPass9!");
        given(userRepository.save(any())).willReturn(photographer);

        RemindPasswordCommand cmd = new RemindPasswordCommand("photo@photodrive.pl", tokenUUID, "NewPass9!");

        // When / Then
        assertThatCode(() -> service.remindPassword(cmd)).doesNotThrowAnyException();
        then(passwordTokenRepository).should().delete(passwordToken);
        then(userRepository).should().save(photographer);
    }

    @Test
    @DisplayName("Password reset fails when no code was ever issued")
    void shouldThrowWhenPasswordTokenNotFound() {
        // Given
        given(userRepository.findByEmail(any())).willReturn(Optional.of(photographer));
        given(passwordTokenRepository.findByUserId(any())).willReturn(Optional.empty());

        RemindPasswordCommand cmd = new RemindPasswordCommand("photo@photodrive.pl", UUID.randomUUID(), "NewPass9!");

        // When / Then - a generic message that does not reveal the token state (anti-enumeration)
        assertThatThrownBy(() -> service.remindPassword(cmd))
                .isInstanceOf(PasswordTokenException.class)
                .hasMessageContaining("Nieprawidłowy lub wygasły");
    }

    @Test
    @DisplayName("Reset returns the same error for a known and an unknown email, so accounts cannot be enumerated")
    void shouldNotRevealWhetherEmailExistsOnRemindPassword() {
        // Given - an unknown email (no such account)
        given(userRepository.findByEmail(any())).willReturn(Optional.empty());

        RemindPasswordCommand cmd = new RemindPasswordCommand("ghost@photodrive.pl", UUID.randomUUID(), "NewPass9!");

        // When / Then - EXACTLY the same response as for a known email without a token,
        // so an attacker cannot tell an existing account from a missing one.
        assertThatThrownBy(() -> service.remindPassword(cmd))
                .isInstanceOf(PasswordTokenException.class)
                .hasMessageContaining("Nieprawidłowy lub wygasły");
    }

    @Test
    @DisplayName("Expired authorization code is rejected")
    void shouldThrowWhenTokenIsExpired() {
        // Given
        UUID tokenUUID = UUID.randomUUID();
        PasswordToken expiredToken = PasswordToken.create(
                tokenUUID,
                Instant.now().minusSeconds(1), // already expired
                Instant.now().minusSeconds(1000),
                photographer);

        given(userRepository.findByEmail(any())).willReturn(Optional.of(photographer));
        given(passwordTokenRepository.findByUserId(any())).willReturn(Optional.of(expiredToken));

        RemindPasswordCommand cmd = new RemindPasswordCommand("photo@photodrive.pl", tokenUUID, "NewPass9!");

        // When / Then
        assertThatThrownBy(() -> service.remindPassword(cmd))
                .isInstanceOf(PasswordTokenException.class)
                .hasMessageContaining("Nieprawidłowy lub wygasły");
    }

    @Test
    @DisplayName("Wrong authorization code is rejected")
    void shouldThrowWhenTokenDoesNotMatch() {
        // Given
        UUID correctToken = UUID.randomUUID();
        UUID wrongToken = UUID.randomUUID();

        PasswordToken passwordToken = PasswordToken.create(
                correctToken,
                Instant.now().plusSeconds(900),
                Instant.now(),
                photographer);

        given(userRepository.findByEmail(any())).willReturn(Optional.of(photographer));
        given(passwordTokenRepository.findByUserId(any())).willReturn(Optional.of(passwordToken));

        RemindPasswordCommand cmd = new RemindPasswordCommand("photo@photodrive.pl", wrongToken, "NewPass9!");

        // When / Then - a generic message (anti-enumeration)
        assertThatThrownBy(() -> service.remindPassword(cmd))
                .isInstanceOf(PasswordTokenException.class)
                .hasMessageContaining("Nieprawidłowy lub wygasły");
    }
}
