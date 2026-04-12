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
import pl.photodrive.core.application.port.repository.PasswordTokenRepository;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.domain.exception.UserException;
import pl.photodrive.core.domain.model.PasswordToken;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.HashedPassword;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokenManagementServiceTest {

    @Mock private PasswordTokenRepository passwordTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TokenManagementService service;

    private User photographer;

    @BeforeEach
    void setUp() {
        photographer = User.create("Photo", new Email("photo@photodrive.pl"),
                new HashedPassword("hashed"), Role.PHOTOGRAPHER);
    }

    // -----------------------------------------------------------------------
    // createToken — new token creation
    // -----------------------------------------------------------------------

    @Test
    void shouldCreateNewTokenWhenNoneExists() {
        // Given
        given(userRepository.findByEmail(new Email("photo@photodrive.pl"))).willReturn(Optional.of(photographer));
        given(passwordTokenRepository.existsByUserId(photographer.getId())).willReturn(false);
        given(passwordTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // When
        service.createToken("photo@photodrive.pl");

        // Then
        verify(passwordTokenRepository).save(any(PasswordToken.class));
        verify(eventPublisher, atLeastOnce()).publishEvent(any(Object.class));
    }

    @Test
    void shouldUpdateExistingTokenWhenOneAlreadyExists() {
        // Given
        UUID existingUUID = UUID.randomUUID();
        PasswordToken existingToken = PasswordToken.create(
                existingUUID,
                Instant.now().plusSeconds(900),
                Instant.now(),
                photographer);

        given(userRepository.findByEmail(new Email("photo@photodrive.pl"))).willReturn(Optional.of(photographer));
        given(passwordTokenRepository.existsByUserId(photographer.getId())).willReturn(true);
        given(passwordTokenRepository.findByUserId(photographer.getId())).willReturn(Optional.of(existingToken));
        given(passwordTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // When
        service.createToken("photo@photodrive.pl");

        // Then
        verify(passwordTokenRepository).save(existingToken);
        verify(eventPublisher, atLeastOnce()).publishEvent(any(Object.class));
    }

    @Test
    void shouldSilentlyReturnWhenUserNotFoundByEmail() {
        // Given
        given(userRepository.findByEmail(any())).willReturn(Optional.empty());

        // When / Then — should not throw, silently returns to prevent account enumeration
        assertThatCode(() -> service.createToken("unknown@photodrive.pl"))
                .doesNotThrowAnyException();
        verify(passwordTokenRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenTokenExistsButCannotBeFoundForUpdate() {
        // Given
        given(userRepository.findByEmail(any())).willReturn(Optional.of(photographer));
        given(passwordTokenRepository.existsByUserId(photographer.getId())).willReturn(true);
        given(passwordTokenRepository.findByUserId(photographer.getId())).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.createToken("photo@photodrive.pl"))
                .isInstanceOf(UserException.class);
    }

    @Test
    void shouldPublishPasswordTokenCreatedEvent() {
        // Given
        given(userRepository.findByEmail(any())).willReturn(Optional.of(photographer));
        given(passwordTokenRepository.existsByUserId(photographer.getId())).willReturn(false);
        given(passwordTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // When
        service.createToken("photo@photodrive.pl");

        // Then - event should be published when token is created
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
    }
}
