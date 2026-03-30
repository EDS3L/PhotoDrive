package pl.photodrive.core.domain.vo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.photodrive.core.domain.exception.FileException;

import static org.junit.jupiter.api.Assertions.*;

class FileNameTest {

    @Test
    void shouldCreateFileNameWithValidJpg() {
        assertDoesNotThrow(() -> new FileName("photo.jpg"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "photo.jpg",
            "photo.jpeg",
            "image.png",
            "animation.gif",
            "scan.bmp",
            "picture.webp",
            "raw.tiff",
            "video.mp4",
            "clip.mov",
            "film.avi",
            "movie.mkv",
            "record.wmv",
            "stream.flv",
            "web.webm",
            "encoded.mpeg",
            "legacy.mpg",
            "apple.heic"
    })
    void shouldCreateFileNameForAllAllowedExtensions(String name) {
        assertDoesNotThrow(() -> new FileName(name));
    }

    @Test
    void shouldCreateFileNameWithSpacesInName() {
        assertDoesNotThrow(() -> new FileName("my photo 2024.jpg"));
    }

    @Test
    void shouldCreateFileNameWithHyphenAndUnderscore() {
        assertDoesNotThrow(() -> new FileName("my-photo_final.jpg"));
    }

    @Test
    void shouldCreateFileNameWithUpperCaseExtension() {
        assertDoesNotThrow(() -> new FileName("photo.JPG"));
    }


    @Test
    void shouldThrowWhenFileNameIsNull() {
        assertThrows(FileException.class, () -> new FileName(null));
    }

    @Test
    void shouldThrowWhenFileNameIsEmpty() {
        assertThrows(FileException.class, () -> new FileName(""));
    }

    @Test
    void shouldThrowWhenFileNameExceeds255Characters() {
        // 252 znaki + ".jpg" = 256
        String longName = "a".repeat(252) + ".jpg";
        assertThrows(FileException.class, () -> new FileName(longName));
    }

    @Test
    void shouldCreateFileNameWithExactly255Characters() {
        // 251 znaki + ".jpg" = 255
        String maxName = "a".repeat(251) + ".jpg";
        assertDoesNotThrow(() -> new FileName(maxName));
    }

    @Test
    void shouldThrowWhenFileNameEndsWithSpace() {
        assertThrows(FileException.class, () -> new FileName("photo.jpg "));
    }

    @Test
    void shouldThrowWhenFileNameEndsWithDot() {
        assertThrows(FileException.class, () -> new FileName("photo.jpg."));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "photo<name>.jpg",
            "photo>name.jpg",
            "photo:name.jpg",
            "photo\"name.jpg",
            "photo/name.jpg",
            "photo\\name.jpg",
            "photo|name.jpg",
            "photo?name.jpg",
            "photo*name.jpg",
            "photo#name.jpg",
            "photo%name.jpg",
            "photo&name.jpg",
            "photo{name}.jpg",
            "photo$name.jpg",
            "photo!name.jpg",
            "photo'name.jpg",
            "photo@name.jpg",
            "photo+name.jpg",
            "photo`name.jpg",
            "photo~name.jpg",
            "photo=name.jpg",
            "photo,name.jpg",
            "photo;name.jpg",
            "photo[name].jpg"
    })
    void shouldThrowWhenFileNameContainsIllegalCharacter(String name) {
        assertThrows(FileException.class, () -> new FileName(name));
    }

    @Test
    void shouldThrowWhenFileNameContainsNullCharacter() {
        assertThrows(FileException.class, () -> new FileName("photo\0name.jpg"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CON.jpg",
            "PRN.jpg",
            "AUX.jpg",
            "NUL.jpg",
            "COM1.jpg",
            "COM9.jpg",
            "LPT1.jpg",
            "LPT9.jpg"
    })
    void shouldThrowWhenFileNameUsesReservedWindowsName(String name) {
        assertThrows(FileException.class, () -> new FileName(name));
    }

    @Test
    void shouldThrowWhenReservedNameIsLowerCase() {
        // Walidacja powinna być case-insensitive
        assertThrows(FileException.class, () -> new FileName("con.jpg"));
    }

    @Test
    void shouldThrowWhenReservedNameIsMixedCase() {
        assertThrows(FileException.class, () -> new FileName("Con.jpg"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "virus.exe",
            "script.bat",
            "run.sh",
            "cmd.cmd",
            "prog.com",
            "setup.msi",
            "hack.scr",
            "app.jar",
            "macro.ps1",
            "code.vb",
            "code.vbs"
    })
    void shouldThrowWhenFileNameHasForbiddenExtension(String name) {
        assertThrows(FileException.class, () -> new FileName(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "document.pdf",
            "spreadsheet.xlsx",
            "archive.zip",
            "text.txt",
            "data.json",
            "photo.raw"
    })
    void shouldThrowWhenFileNameHasUnsupportedExtension(String name) {
        assertThrows(FileException.class, () -> new FileName(name));
    }

    @Test
    void shouldThrowWhenFileNameHasNoExtension() {
        assertThrows(FileException.class, () -> new FileName("photoWithoutExtension"));
    }

    @Test
    void shouldBeEqualWhenSameName() {
        FileName a = new FileName("photo.jpg");
        FileName b = new FileName("photo.jpg");

        assertEquals(a, b);
    }

    @Test
    void shouldNotBeEqualWhenDifferentName() {
        FileName a = new FileName("photo.jpg");
        FileName b = new FileName("other.jpg");

        assertNotEquals(a, b);
    }

    @Test
    void shouldHaveSameHashCodeWhenEqual() {
        FileName a = new FileName("photo.jpg");
        FileName b = new FileName("photo.jpg");

        assertEquals(a.hashCode(), b.hashCode());
    }
}