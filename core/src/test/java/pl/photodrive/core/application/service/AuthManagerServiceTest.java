package pl.photodrive.core.application.service;

import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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
    void shouldReturnAccessTokenOnSuccessfulLogin() {
        // Given
        given(userRepository.findByEmail(any())).willReturn(Optional.of(photographer));
        given(passwordHasher.matches(any(), any())).willReturn(true);
        given(tokenEncoder.createAccessToken(any(), any(), any(), any())).willReturn("jwt.token.here");

        LoginCommand cmd = new LoginCommand("photo@photodrive.pl", "Pass1!");

        // When
        AccessToken token = service.login(cmd);

        // Then
        assertThat(token.value()).isEqualTo("jwt.token.here");
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        // Given
        given(userRepository.findByEmail(any())).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.login(new LoginCommand("unknown@photodrive.pl", "Pass1!")))
                .isInstanceOf(LoginFailedException.class);
    }

    @Test
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
    void shouldThrowWhenPasswordDoesNotMatch() {
        // Given
        given(userRepository.findByEmail(any())).willReturn(Optional.of(photographer));
        given(passwordHasher.matches(any(), any())).willReturn(false);

        // When / Then
        assertThatThrownBy(() -> service.login(new LoginCommand("photo@photodrive.pl", "WrongPass!")))
                .isInstanceOf(LoginFailedException.class);
    }

    @Test
    void shouldReturnAccessTokenEvenWhenChangePasswordFlagIsSet() {
        // Given — użytkownik z flagą „zmień hasło" MUSI móc się zalogować hasłem startowym;
        // wymuszenie zmiany hasła realizuje front (bramka wg /user/me), nie backend.
        photographer.setChangePasswordOnNextLogin(true);
        given(userRepository.findByEmail(any())).willReturn(Optional.of(photographer));
        given(passwordHasher.matches(any(), any())).willReturn(true);
        given(tokenEncoder.createAccessToken(any(), any(), any(), any())).willReturn("jwt.token.here");

        // When
        AccessToken token = service.login(new LoginCommand("photo@photodrive.pl", "Pass1!"));

        // Then
        assertThat(token.value()).isEqualTo("jwt.token.here");
    }

    // -----------------------------------------------------------------------
    // remindPassword
    // -----------------------------------------------------------------------

    @Test
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
        verify(passwordTokenRepository).delete(passwordToken);
        verify(userRepository).save(photographer);
    }

    @Test
    void shouldThrowWhenPasswordTokenNotFound() {
        // Given
        given(userRepository.findByEmail(any())).willReturn(Optional.of(photographer));
        given(passwordTokenRepository.findByUserId(any())).willReturn(Optional.empty());

        RemindPasswordCommand cmd = new RemindPasswordCommand("photo@photodrive.pl", UUID.randomUUID(), "NewPass9!");

        // When / Then — generyczny komunikat (anty-enumeracja), nie ujawnia stanu tokenu
        assertThatThrownBy(() -> service.remindPassword(cmd))
                .isInstanceOf(PasswordTokenException.class)
                .hasMessageContaining("Nieprawidłowy lub wygasły");
    }

    @Test
    void shouldNotRevealWhetherEmailExistsOnRemindPassword() {
        // Given — nieznany email (konto nie istnieje)
        given(userRepository.findByEmail(any())).willReturn(Optional.empty());

        RemindPasswordCommand cmd = new RemindPasswordCommand("ghost@photodrive.pl", UUID.randomUUID(), "NewPass9!");

        // When / Then — DOKŁADNIE ta sama odpowiedź co przy znanym emailu bez tokenu,
        // więc atakujący nie odróżni istniejącego konta od nieistniejącego.
        assertThatThrownBy(() -> service.remindPassword(cmd))
                .isInstanceOf(PasswordTokenException.class)
                .hasMessageContaining("Nieprawidłowy lub wygasły");
    }

    @Test
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

        // When / Then — generyczny komunikat (anty-enumeracja)
        assertThatThrownBy(() -> service.remindPassword(cmd))
                .isInstanceOf(PasswordTokenException.class)
                .hasMessageContaining("Nieprawidłowy lub wygasły");
    }
}
