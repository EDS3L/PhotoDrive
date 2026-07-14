package pl.photodrive.core.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.photodrive.core.domain.exception.FileException;

import static org.junit.jupiter.api.Assertions.*;

class FileNameTest {

    @Test
    @DisplayName("File name accepts a plain JPG")
    void shouldCreateFileNameWithValidJpg() {
        // When / Then
        assertDoesNotThrow(() -> new FileName("photo.jpg"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "photo.jpg",
            "photo.jpeg",
            "image.png"
    })
    @DisplayName("Every allowed extension is accepted")
    void shouldCreateFileNameForAllAllowedExtensions(String name) {
        // When / Then
        assertDoesNotThrow(() -> new FileName(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"photo.jpg", "photo.jpeg", "image.png"})
    @DisplayName("Upload factory accepts the formats the server can actually decode")
    void shouldCreateWithOfForAllowedFormats(String name) {
        // When / Then
        assertDoesNotThrow(() -> FileName.of(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // Reconstitution from the database (the constructor) tolerates formats from the older, wider policy,
            // otherwise legacy rows would fail to load. The whitelist is enforced by of() only.
            "legacy.webp",
            "old.heic",
            "scan.tiff",
            "clip.mp4"
    })
    @DisplayName("Legacy formats still load from the database, so old rows are not lost")
    void shouldAcceptLegacyFormatsOnReconstitution(String name) {
        // When / Then
        assertDoesNotThrow(() -> new FileName(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // Image formats stock ImageIO cannot read (null -> NPE -> 500), so they are rejected on upload
            "picture.webp",
            "apple.heic",
            "raw.tiff",
            "scan.bmp",
            "animation.gif",
            // Video - PhotoDrive is a photo platform, recordings are out of scope
            "video.mp4",
            "clip.mov",
            "film.avi",
            "movie.mkv",
            "record.wmv",
            "stream.flv",
            "web.webm",
            "encoded.mpeg",
            "legacy.mpg"
    })
    @DisplayName("Upload rejects formats the server cannot decode, instead of failing later on preview")
    void shouldThrowWhenCreatingNewWithUnreadableImageOrVideoFormat(String name) {
        // When / Then
        assertThrows(FileException.class, () -> FileName.of(name));
    }

    @Test
    @DisplayName("File name may contain spaces")
    void shouldCreateFileNameWithSpacesInName() {
        // When / Then
        assertDoesNotThrow(() -> new FileName("my photo 2024.jpg"));
    }

    @Test
    @DisplayName("File name may contain a hyphen and an underscore")
    void shouldCreateFileNameWithHyphenAndUnderscore() {
        // When / Then
        assertDoesNotThrow(() -> new FileName("my-photo_final.jpg"));
    }

    @Test
    @DisplayName("Extension is matched case-insensitively")
    void shouldCreateFileNameWithUpperCaseExtension() {
        // When / Then
        assertDoesNotThrow(() -> new FileName("photo.JPG"));
    }


    @Test
    @DisplayName("File name cannot be null")
    void shouldThrowWhenFileNameIsNull() {
        // When / Then
        assertThrows(FileException.class, () -> new FileName(null));
    }

    @Test
    @DisplayName("File name cannot be empty")
    void shouldThrowWhenFileNameIsEmpty() {
        // When / Then
        assertThrows(FileException.class, () -> new FileName(""));
    }

    @Test
    @DisplayName("File name longer than the file-system limit is rejected")
    void shouldThrowWhenFileNameExceeds255Characters() {
        // Given
        // 252 characters + ".jpg" = 256
        String longName = "a".repeat(252) + ".jpg";

        // When / Then
        assertThrows(FileException.class, () -> new FileName(longName));
    }

    @Test
    @DisplayName("File name of exactly the maximum length is still accepted")
    void shouldCreateFileNameWithExactly255Characters() {
        // Given
        // 251 characters + ".jpg" = 255
        String maxName = "a".repeat(251) + ".jpg";

        // When / Then
        assertDoesNotThrow(() -> new FileName(maxName));
    }

    @Test
    @DisplayName("File name cannot end with a space")
    void shouldThrowWhenFileNameEndsWithSpace() {
        // When / Then
        assertThrows(FileException.class, () -> new FileName("photo.jpg "));
    }

    @Test
    @DisplayName("File name cannot end with a dot")
    void shouldThrowWhenFileNameEndsWithDot() {
        // When / Then
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
    @DisplayName("File name rejects characters illegal in a file system")
    void shouldThrowWhenFileNameContainsIllegalCharacter(String name) {
        // When / Then
        assertThrows(FileException.class, () -> new FileName(name));
    }

    @Test
    @DisplayName("File name rejects a NUL byte, a classic path-injection trick")
    void shouldThrowWhenFileNameContainsNullCharacter() {
        // When / Then
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
    @DisplayName("Reserved Windows device names are rejected")
    void shouldThrowWhenFileNameUsesReservedWindowsName(String name) {
        // When / Then
        assertThrows(FileException.class, () -> new FileName(name));
    }

    @Test
    @DisplayName("Reserved names are rejected regardless of case")
    void shouldThrowWhenReservedNameIsLowerCase() {
        // When / Then - reserved names are matched case-insensitively
        assertThrows(FileException.class, () -> new FileName("con.jpg"));
    }

    @Test
    @DisplayName("Reserved names are rejected in mixed case as well")
    void shouldThrowWhenReservedNameIsMixedCase() {
        // When / Then
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
    @DisplayName("Executable extensions are rejected")
    void shouldThrowWhenFileNameHasForbiddenExtension(String name) {
        // When / Then
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
    @DisplayName("Extension outside the allowed list is rejected")
    void shouldThrowWhenFileNameHasUnsupportedExtension(String name) {
        // When / Then
        assertThrows(FileException.class, () -> FileName.of(name));
    }

    @Test
    @DisplayName("File name without an extension is rejected")
    void shouldThrowWhenFileNameHasNoExtension() {
        // When / Then
        assertThrows(FileException.class, () -> FileName.of("photoWithoutExtension"));
    }

    @Test
    @DisplayName("Two file names with the same value are equal")
    void shouldBeEqualWhenSameName() {
        // Given
        FileName a = new FileName("photo.jpg");

        // When
        FileName b = new FileName("photo.jpg");

        // Then
        assertEquals(a, b);
    }

    @Test
    @DisplayName("Different file names are not equal")
    void shouldNotBeEqualWhenDifferentName() {
        // Given
        FileName a = new FileName("photo.jpg");

        // When
        FileName b = new FileName("other.jpg");

        // Then
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("Equal file names share a hash code, so they work as map keys")
    void shouldHaveSameHashCodeWhenEqual() {
        // Given
        FileName a = new FileName("photo.jpg");

        // When
        FileName b = new FileName("photo.jpg");

        // Then
        assertEquals(a.hashCode(), b.hashCode());
    }
}