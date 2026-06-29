package pl.photodrive.core.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.photodrive.core.domain.event.user.PasswordTokenCreated;
import pl.photodrive.core.domain.exception.PasswordTokenException;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.HashedPassword;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class PasswordTokenTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = User.create("TestUser", new Email("user@example.com"), new HashedPassword("hashed"), Role.PHOTOGRAPHER);
    }

    // -----------------------------------------------------------------------
    // PasswordToken.create
    // -----------------------------------------------------------------------

    @Test
    void shouldCreatePasswordTokenWithCorrectData() {
        // Given
        UUID token = UUID.randomUUID();
        Instant expiration = Instant.now().plusSeconds(900);
        Instant created = Instant.now();

        // When
        PasswordToken passwordToken = PasswordToken.create(token, expiration, created, user);

        // Then
        assertThat(passwordToken).isNotNull();
        assertThat(passwordToken.matches(token)).isTrue();
        assertThat(passwordToken.matches(UUID.randomUUID())).isFalse();
        assertThat(passwordToken.getTokenHash()).hasSize(64);
        assertThat(passwordToken.getTokenHash()).isNotEqualTo(token.toString());
        assertThat(passwordToken.getUserId()).isEqualTo(user.getId());
    }

    @Test
    void shouldRegisterPasswordTokenCreatedEvent() {
        // Given
        UUID token = UUID.randomUUID();

        // When
        PasswordToken passwordToken = PasswordToken.create(token, Instant.now().plusSeconds(900), Instant.now(), user);

        // Then
        List<Object> events = passwordToken.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PasswordTokenCreated.class);
        PasswordTokenCreated event = (PasswordTokenCreated) events.get(0);
        assertThat(event.email()).isEqualTo(user.getEmail().value());
        assertThat(event.token()).isEqualTo(token);
    }

    @Test
    void shouldThrowWhenTokenIsNull() {
        assertThatThrownBy(() -> PasswordToken.create(null, Instant.now().plusSeconds(900), Instant.now(), user))
                .isInstanceOf(PasswordTokenException.class)
                .hasMessageContaining("Token is null");
    }

    @Test
    void shouldThrowWhenExpirationIsNull() {
        assertThatThrownBy(() -> PasswordToken.create(UUID.randomUUID(), null, Instant.now(), user))
                .isInstanceOf(PasswordTokenException.class)
                .hasMessageContaining("Expiration is null");
    }

    @Test
    void shouldThrowWhenCreatedIsNull() {
        assertThatThrownBy(() -> PasswordToken.create(UUID.randomUUID(), Instant.now().plusSeconds(900), null, user))
                .isInstanceOf(PasswordTokenException.class)
                .hasMessageContaining("Created is null");
    }

    // -----------------------------------------------------------------------
    // updateToken
    // -----------------------------------------------------------------------

    @Test
    void shouldUpdateTokenAndUpdateExpiration() {
        // Given
        UUID original = UUID.randomUUID();
        PasswordToken passwordToken = PasswordToken.create(original, Instant.now().plusSeconds(900), Instant.now(), user);
        passwordToken.pullDomainEvents(); // clear initial event

        UUID newToken = UUID.randomUUID();
        String originalHash = passwordToken.getTokenHash();

        // When
        passwordToken.updateToken(newToken, user.getEmail().value());

        // Then
        assertThat(passwordToken.matches(newToken)).isTrue();
        assertThat(passwordToken.matches(original)).isFalse();
        assertThat(passwordToken.getTokenHash()).isNotEqualTo(originalHash);
        List<Object> events = passwordToken.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PasswordTokenCreated.class);
    }

    @Test
    void shouldThrowWhenUpdatingWithNullToken() {
        // Given
        PasswordToken passwordToken = PasswordToken.create(UUID.randomUUID(), Instant.now().plusSeconds(900), Instant.now(), user);

        // When / Then
        assertThatThrownBy(() -> passwordToken.updateToken(null, user.getEmail().value()))
                .isInstanceOf(PasswordTokenException.class)
                .hasMessageContaining("Token is null");
    }

    @Test
    void shouldThrowWhenUpdatingWithSameToken() {
        // Given
        UUID original = UUID.randomUUID();
        PasswordToken passwordToken = PasswordToken.create(original, Instant.now().plusSeconds(900), Instant.now(), user);

        // When / Then
        assertThatThrownBy(() -> passwordToken.updateToken(original, user.getEmail().value()))
                .isInstanceOf(PasswordTokenException.class)
                .hasMessageContaining("same");
    }

    @Test
    void shouldClearEventsAfterPull() {
        // Given
        PasswordToken passwordToken = PasswordToken.create(UUID.randomUUID(), Instant.now().plusSeconds(900), Instant.now(), user);

        // When
        passwordToken.pullDomainEvents();

        // Then - second pull should be empty
        assertThat(passwordToken.pullDomainEvents()).isEmpty();
    }
}
