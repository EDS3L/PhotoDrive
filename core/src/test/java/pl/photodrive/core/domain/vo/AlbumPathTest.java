package pl.photodrive.core.domain.vo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class AlbumPathTest {

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowWhenNullOrEmpty(String value) {
        assertThatThrownBy(() -> new AlbumPath(value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenStartsWithSlash() {
        assertThatThrownBy(() -> new AlbumPath("/absolute/path"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot start with '/'");
    }

    @Test
    void shouldThrowWhenEndsWithSlash() {
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
    void shouldThrowOnPathTraversal(String value) {
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
    void shouldThrowOnInvalidCharacters(String value) {
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
    void shouldAcceptValidPaths(String value) {
        assertThatCode(() -> new AlbumPath(value)).doesNotThrowAnyException();
    }

    @Test
    void shouldReturnValue() {
        AlbumPath path = new AlbumPath("my/album");
        assertThat(path.value()).isEqualTo("my/album");
    }

    @Test
    void shouldBeEqualWhenSameValue() {
        assertThat(new AlbumPath("my/album")).isEqualTo(new AlbumPath("my/album"));
    }
}
