package pl.photodrive.core.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.photodrive.core.domain.exception.UserException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class AlbumIdTest {

    @Test
    @DisplayName("Album id wraps a valid UUID")
    void shouldCreateWithValidUUID() {
        // Given
        UUID uuid = UUID.randomUUID();

        // When
        AlbumId albumId = new AlbumId(uuid);

        // Then
        assertThat(albumId.value()).isEqualTo(uuid);
    }

    @Test
    @DisplayName("Album id cannot be null")
    void shouldThrowWhenNull() {
        // When / Then
        assertThatThrownBy(() -> new AlbumId(null))
                .isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("Each generated album id is unique")
    void shouldGenerateNewUniqueId() {
        // Given
        AlbumId id1 = AlbumId.newId();

        // When
        AlbumId id2 = AlbumId.newId();

        // Then
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("Two album ids with the same UUID are equal")
    void shouldBeEqualWhenSameUUID() {
        // When
        UUID uuid = UUID.randomUUID();

        // Then
        assertThat(new AlbumId(uuid)).isEqualTo(new AlbumId(uuid));
    }

    @Test
    @DisplayName("Equal album ids share a hash code, so they work as map keys")
    void shouldHaveSameHashCodeForEqualObjects() {
        // When
        UUID uuid = UUID.randomUUID();

        // Then
        assertThat(new AlbumId(uuid).hashCode()).isEqualTo(new AlbumId(uuid).hashCode());
    }
}
