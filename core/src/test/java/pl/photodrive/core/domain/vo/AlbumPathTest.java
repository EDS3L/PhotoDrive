package pl.photodrive.core.domain.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AlbumPathTest {
    @Test
    void shouldCreateValidPath() {
        assertDoesNotThrow(() -> new AlbumPath("photos/summer-2024"));
    }

    @Test
    void shouldThrowForTraversalAttempt() {
        assertThrows(IllegalArgumentException.class, () -> new AlbumPath("../etc/passwd"));
    }

    @Test
    void shouldThrowWhenStartsWithSlash() {
        assertThrows(IllegalArgumentException.class, () -> new AlbumPath("/absolute/path"));
    }
}