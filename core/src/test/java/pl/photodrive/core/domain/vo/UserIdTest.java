package pl.photodrive.core.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.photodrive.core.domain.exception.UserException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class UserIdTest {

    @Test
    @DisplayName("User id wraps a valid UUID")
    void shouldCreateWithValidUUID() {
        // Given
        UUID uuid = UUID.randomUUID();

        // When
        UserId userId = new UserId(uuid);

        // Then
        assertThat(userId.value()).isEqualTo(uuid);
    }

    @Test
    @DisplayName("User id cannot be null")
    void shouldThrowWhenNull() {
        // When / Then
        assertThatThrownBy(() -> new UserId(null))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("Each generated user id is unique")
    void shouldGenerateNewUniqueId() {
        // Given
        UserId id1 = UserId.newId();

        // When
        UserId id2 = UserId.newId();

        // Then
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("Two user ids with the same UUID are equal")
    void shouldBeEqualWhenSameUUID() {
        // When
        UUID uuid = UUID.randomUUID();

        // Then
        assertThat(new UserId(uuid)).isEqualTo(new UserId(uuid));
    }

    @Test
    @DisplayName("Equal user ids share a hash code, so they work as map keys")
    void shouldHaveSameHashCodeForEqualObjects() {
        // When
        UUID uuid = UUID.randomUUID();

        // Then
        assertThat(new UserId(uuid).hashCode()).isEqualTo(new UserId(uuid).hashCode());
    }
}
