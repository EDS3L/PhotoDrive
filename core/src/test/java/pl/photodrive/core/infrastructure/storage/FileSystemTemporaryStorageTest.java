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
 * Magazyn tymczasowy przyjmuje bajty uploadu, zanim transakcja się zacommituje.
 * Handler zapisu (BEFORE_COMMIT) polega na exists/getFile/delete.
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
    @DisplayName("Zapisany plik tymczasowy da się odczytać i skasować")
    void shouldSaveReadAndDeleteTemporaryFile() throws IOException {
        String tempId = storage.saveTemporary(new ByteArrayInputStream("zdjecie".getBytes()));

        assertThat(tempId).isNotBlank();
        assertThat(storage.exists(tempId)).isTrue();

        try (InputStream in = storage.getFile(tempId)) {
            assertThat(new String(in.readAllBytes())).isEqualTo("zdjecie");
        }

        storage.delete(tempId);
        assertThat(storage.exists(tempId)).isFalse();
    }

    @Test
    @DisplayName("Katalog tymczasowy powstaje, gdy jeszcze nie istnieje")
    void shouldCreateTempDirectoryIfMissing() {
        Path nested = tempDir.resolve("glebiej/temp");

        new FileSystemTemporaryStorage(nested.toString());

        assertThat(Files.isDirectory(nested)).isTrue();
    }

    @Test
    @DisplayName("Nieistniejący plik tymczasowy zgłasza błąd zamiast zwracać pustkę")
    void shouldThrowWhenTemporaryFileMissing() {
        assertThatThrownBy(() -> storage.getFile("nie-ma-takiego"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("Kasowanie nieistniejącego pliku jest bezpieczne (idempotentne)")
    void shouldIgnoreDeleteOfMissingFile() {
        storage.delete("nie-ma-takiego");

        assertThat(storage.exists("nie-ma-takiego")).isFalse();
    }

    @Test
    @DisplayName("Path traversal w tempId jest odrzucany")
    void shouldRejectPathTraversal() {
        assertThatThrownBy(() -> storage.exists("../../etc/passwd"))
                .isInstanceOf(StorageException.class);

        assertThatThrownBy(() -> storage.getFile("../poza-katalogiem"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("Katalog nie jest plikiem tymczasowym")
    void shouldNotTreatDirectoryAsTemporaryFile() throws IOException {
        Files.createDirectory(tempDir.resolve("podkatalog"));

        assertThat(storage.exists("podkatalog")).isFalse();
    }
}
