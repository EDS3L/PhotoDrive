package pl.photodrive.core.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.photodrive.core.domain.exception.UserException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class FileIdTest {

    @Test
    @DisplayName("File id wraps a valid UUID")
    void shouldCreateWithValidUUID() {
        // Given
        UUID uuid = UUID.randomUUID();

        // When
        FileId fileId = new FileId(uuid);

        // Then
        assertThat(fileId.value()).isEqualTo(uuid);
    }

    @Test
    @DisplayName("File id cannot be null")
    void shouldThrowWhenNull() {
        // When / Then
        assertThatThrownBy(() -> new FileId(null))
                .isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("Each generated file id is unique")
    void shouldGenerateNewUniqueRawUUID() {
        // Given
        UUID id1 = FileId.newId();

        // When
        UUID id2 = FileId.newId();

        // Then
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("Two file ids with the same UUID are equal")
    void shouldBeEqualWhenSameUUID() {
        // When
        UUID uuid = UUID.randomUUID();

        // Then
        assertThat(new FileId(uuid)).isEqualTo(new FileId(uuid));
    }

    @Test
    @DisplayName("Equal file ids share a hash code, so they work as map keys")
    void shouldHaveSameHashCodeForEqualObjects() {
        // When
        UUID uuid = UUID.randomUUID();

        // Then
        assertThat(new FileId(uuid).hashCode()).isEqualTo(new FileId(uuid).hashCode());
    }
}
