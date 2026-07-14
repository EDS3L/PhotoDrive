package pl.photodrive.core.infrastructure.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.photodrive.core.infrastructure.exception.StorageException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Temporary storage holds the uploaded bytes until the transaction commits.
 * The BEFORE_COMMIT storage handler relies on exists/getFile/delete.
 */
class FileSystemTemporaryStorageTest {

    @TempDir
    Path tempDir;

    private FileSystemTemporaryStorage storage;

    @BeforeEach
    void setUp() {
        storage = new FileSystemTemporaryStorage(tempDir.toString());
    }

    @Test
    @DisplayName("A temporary file can be saved, read back and deleted")
    void shouldSaveReadAndDeleteTemporaryFile() throws IOException {
        // When
        String tempId = storage.saveTemporary(new ByteArrayInputStream("zdjecie".getBytes()));

        // Then
        assertThat(tempId).isNotBlank();
        assertThat(storage.exists(tempId)).isTrue();

        try (InputStream in = storage.getFile(tempId)) {
            assertThat(new String(in.readAllBytes())).isEqualTo("zdjecie");
        }

        storage.delete(tempId);
        assertThat(storage.exists(tempId)).isFalse();
    }

    @Test
    @DisplayName("The temporary directory is created when it does not exist")
    void shouldCreateTempDirectoryIfMissing() {
        // Given
        Path nested = tempDir.resolve("glebiej/temp");

        // When
        new FileSystemTemporaryStorage(nested.toString());

        // Then
        assertThat(Files.isDirectory(nested)).isTrue();
    }

    @Test
    @DisplayName("Reading a missing temporary file fails loudly instead of returning nothing")
    void shouldThrowWhenTemporaryFileMissing() {
        // When / Then
        assertThatThrownBy(() -> storage.getFile("nie-ma-takiego"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("Deleting a missing temporary file is harmless")
    void shouldIgnoreDeleteOfMissingFile() {
        // When
        storage.delete("nie-ma-takiego");

        // Then
        assertThat(storage.exists("nie-ma-takiego")).isFalse();
    }

    @Test
    @DisplayName("Path traversal in the temporary file id is rejected")
    void shouldRejectPathTraversal() {
        // When / Then
        assertThatThrownBy(() -> storage.exists("../../etc/passwd"))
                .isInstanceOf(StorageException.class);

        assertThatThrownBy(() -> storage.getFile("../poza-katalogiem"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("A directory is not mistaken for a temporary file")
    void shouldNotTreatDirectoryAsTemporaryFile() throws IOException {
        // When
        Files.createDirectory(tempDir.resolve("podkatalog"));

        // Then
        assertThat(storage.exists("podkatalog")).isFalse();
    }
}
