package pl.photodrive.core.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class AlbumPathTest {

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Album path cannot be null or empty")
    void shouldThrowWhenNullOrEmpty(String value) {
        // When / Then
        assertThatThrownBy(() -> new AlbumPath(value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Album path must be relative, so it cannot start with a slash")
    void shouldThrowWhenStartsWithSlash() {
        // When / Then
        assertThatThrownBy(() -> new AlbumPath("/absolute/path"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot start with '/'");
    }

    @Test
    @DisplayName("Album path cannot end with a slash")
    void shouldThrowWhenEndsWithSlash() {
        // When / Then
        assertThatThrownBy(() -> new AlbumPath("valid/path/"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot end with '/'");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "../etc/passwd",
            "valid/../etc",
            "path/../../root",
            "a/..b/../c"
    })
    @DisplayName("Album path rejects traversal sequences, so it cannot escape the storage root")
    void shouldThrowOnPathTraversal(String value) {
        // When / Then
        assertThatThrownBy(() -> new AlbumPath(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("traversal");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "album<name>",
            "album;drop",
            "album|pipe",
            "album\\backslash"
    })
    @DisplayName("Album path rejects characters that are illegal in a file system")
    void shouldThrowOnInvalidCharacters(String value) {
        // When / Then
        assertThatThrownBy(() -> new AlbumPath(value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "my-album",
            "photographer@email.com/album_name",
            "album 2024",
            "client+album/2024-01-01",
            "Album_Name_123"
    })
    @DisplayName("Valid album paths are accepted")
    void shouldAcceptValidPaths(String value) {
        // When / Then
        assertThatCode(() -> new AlbumPath(value)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Album path exposes its raw value")
    void shouldReturnValue() {
        // When
        AlbumPath path = new AlbumPath("my/album");

        // Then
        assertThat(path.value()).isEqualTo("my/album");
    }

    @Test
    @DisplayName("Two album paths with the same value are equal")
    void shouldBeEqualWhenSameValue() {
        // When / Then
        assertThat(new AlbumPath("my/album")).isEqualTo(new AlbumPath("my/album"));
    }
}
