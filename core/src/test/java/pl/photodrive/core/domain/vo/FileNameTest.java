package pl.photodrive.core.domain.vo;

import org.junit.jupiter.api.Test;
import pl.photodrive.core.domain.exception.FileException;

import static org.junit.jupiter.api.Assertions.*;

class FileNameTest {

    @Test
    void shouldCreateValidFileName() {
        assertDoesNotThrow(() -> new FileName("vacation.jpg"));
        assertDoesNotThrow(() -> new FileName("my_photo.png"));
        assertDoesNotThrow(() -> new FileName("IMG_1234.HEIC"));
    }

    @Test
    void shouldThrowExceptionForForbiddenExtension() {
        assertThrows(FileException.class, () -> new FileName("virus.exe"));
        assertThrows(FileException.class, () -> new FileName("script.sh"));
    }

    @Test
    void shouldThrowExceptionForInvalidCharacters() {
        assertThrows(FileException.class, () -> new FileName("photo<1>.jpg"));
        assertThrows(FileException.class, () -> new FileName("test|file.png"));
    }
}
