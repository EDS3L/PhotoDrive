package pl.photodrive.core.infrastructure.jpa.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.photodrive.core.domain.model.PasswordToken;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.HashedPassword;
import pl.photodrive.core.infrastructure.jpa.entity.PasswordTokenEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mapowanie tokenu resetu hasła. Wygląda na nudne przepisywanie pól, ale siedzi na
 * ścieżce bezpieczeństwa: gdy zgubi hash albo datę wygaśnięcia, reset hasła przestaje
 * działać <b>po cichu</b> — użytkownik dostaje „nieprawidłowy kod" dla poprawnego kodu.
 */
class PasswordTokenEntityMapperTest {

    private static final Instant CREATED = Instant.parse("2026-07-25T10:00:00Z");
    private static final Instant EXPIRATION = CREATED.plus(15, ChronoUnit.MINUTES);

    private static User user() {
        return User.create("Klient",
                new Email("klient@photodrive.pl"),
                new HashedPassword("hashed_pwd"),
                Role.CLIENT);
    }

    @Test
    @DisplayName("A token survives the round trip through the database well enough to still match its raw code, which is the only thing the reset flow asks of it")
    void shouldKeepTokenUsableAfterRoundTrip() {
        // Given - a token created from a raw code; only its SHA-256 hash is ever persisted
        User user = user();
        UUID rawCode = UUID.randomUUID();
        PasswordToken original = PasswordToken.create(rawCode, EXPIRATION, CREATED, user);

        // When - domain -> entity -> domain, exactly what save() does
        PasswordToken restored = PasswordTokenEntityMapper.toDomain(
                PasswordTokenEntityMapper.toEntity(original));

        // Then - the restored token must still recognise the code the user was mailed;
        // this is what breaks (silently) if the hash column is mapped to the wrong field
        assertThat(restored.matches(rawCode)).isTrue();
        assertThat(restored.getTokenHash()).isEqualTo(original.getTokenHash());
        assertThat(restored.getExpiration()).isEqualTo(EXPIRATION);
        assertThat(restored.getCreated()).isEqualTo(CREATED);
        assertThat(restored.getUserId()).isEqualTo(user.getId());
        assertThat(restored.getId()).isEqualTo(original.getId());
    }

    @Test
    @DisplayName("The persisted row holds the hash and never the raw code, so a database leak does not hand over working reset codes")
    void shouldPersistHashNeverTheRawCode() {
        // Given
        UUID rawCode = UUID.randomUUID();
        PasswordToken token = PasswordToken.create(rawCode, EXPIRATION, CREATED, user());

        // When
        PasswordTokenEntity entity = PasswordTokenEntityMapper.toEntity(token);

        // Then - SHA-256 hex is 64 chars, matching the column length; the raw UUID must be absent
        assertThat(entity.getToken())
                .isNotEqualTo(rawCode.toString())
                .doesNotContain(rawCode.toString())
                .hasSize(64);
    }

    @Test
    @DisplayName("A token restored from the database keeps rejecting codes other than its own")
    void shouldStillRejectForeignCodeAfterRoundTrip() {
        // Given
        PasswordToken original = PasswordToken.create(UUID.randomUUID(), EXPIRATION, CREATED, user());

        // When
        PasswordToken restored = PasswordTokenEntityMapper.toDomain(
                PasswordTokenEntityMapper.toEntity(original));

        // Then - a mapper that dropped the hash into a constant would pass the happy-path
        // test above and still let ANY code through here
        assertThat(restored.matches(UUID.randomUUID())).isFalse();
    }
}
