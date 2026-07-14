package pl.photodrive.core.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.photodrive.core.domain.exception.FileException;
import pl.photodrive.core.domain.vo.FileName;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class FileTest {

    // -----------------------------------------------------------------------
    // Factory: File.create
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("New file starts hidden and without a watermark")
    void shouldCreateFileWithDefaultValues() {
        // When
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");

        // Then
        assertThat(file.getFileId()).isNotNull();
        assertThat(file.getFileName().value()).isEqualTo("photo.jpg");
        assertThat(file.getSizeBytes()).isEqualTo(100L);
        assertThat(file.getContentType()).isEqualTo("image/jpeg");
        assertThat(file.isVisible()).isFalse();
        assertThat(file.isHasWatermark()).isFalse();
    }

    @Test
    @DisplayName("A file must have a non-zero size")
    void shouldThrowWhenSizeIsZero() {
        // When / Then
        assertThatThrownBy(() -> File.create(new FileName("photo.jpg"), 0L, "image/jpeg"))
                .isInstanceOf(FileException.class)
                .hasMessageContaining("negative");
    }

    @Test
    @DisplayName("A file cannot have a negative size")
    void shouldThrowWhenSizeIsNegative() {
        // When / Then
        assertThatThrownBy(() -> File.create(new FileName("photo.jpg"), -1L, "image/jpeg"))
                .isInstanceOf(FileException.class);
    }

    // -----------------------------------------------------------------------
    // Constructor validation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("A file must carry a content type")
    void shouldThrowWhenContentTypeIsNull() {
        // When / Then
        assertThatThrownBy(() -> new File(
                new pl.photodrive.core.domain.vo.FileId(java.util.UUID.randomUUID()),
                new FileName("photo.jpg"),
                100L,
                null,
                Instant.now().minusSeconds(1),
                false,
                false))
                .isInstanceOf(FileException.class)
                .hasMessageContaining("Content type");
    }

    @Test
    @DisplayName("Upload time cannot be in the future")
    void shouldThrowWhenUploadedAtIsInFuture() {
        // When / Then
        assertThatThrownBy(() -> new File(
                new pl.photodrive.core.domain.vo.FileId(java.util.UUID.randomUUID()),
                new FileName("photo.jpg"),
                100L,
                "image/jpeg",
                Instant.now().plusSeconds(1000),
                false,
                false))
                .isInstanceOf(FileException.class)
                .hasMessageContaining("Uploaded at");
    }

    // -----------------------------------------------------------------------
    // rename
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("File can be renamed")
    void shouldRenameFileSuccessfully() {
        // Given
        File file = File.create(new FileName("old.jpg"), 100L, "image/jpeg");

        // When
        file.rename(new FileName("new.jpg"));

        // Then
        assertThat(file.getFileName().value()).isEqualTo("new.jpg");
    }

    @Test
    @DisplayName("File cannot be renamed to null")
    void shouldThrowWhenRenamingWithNull() {
        // Given
        File file = File.create(new FileName("old.jpg"), 100L, "image/jpeg");

        // When / Then
        assertThatThrownBy(() -> file.rename(null))
                .isInstanceOf(FileException.class);
    }

    // -----------------------------------------------------------------------
    // setViable / setUnviable
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("File can be revealed to the client")
    void shouldSetFileVisible() {
        // Given
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");

        // When
        file.setViable();

        // Then
        assertThat(file.isVisible()).isTrue();
    }

    @Test
    @DisplayName("Revealing an already visible file is refused (the album layer keeps batches idempotent)")
    void shouldThrowWhenSettingVisibleOnAlreadyVisibleFile() {
        // Given
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        file.setViable();

        // When / Then
        assertThatThrownBy(file::setViable)
                .isInstanceOf(FileException.class)
                .hasMessageContaining("already viable");
    }

    @Test
    @DisplayName("File can be hidden from the client")
    void shouldSetFileUnviable() {
        // Given
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        file.setViable();

        // When
        file.setUnviable();

        // Then
        assertThat(file.isVisible()).isFalse();
    }

    @Test
    @DisplayName("Hiding an already hidden file is refused (the album layer keeps batches idempotent)")
    void shouldThrowWhenSettingUnviableOnAlreadyUnviableFile() {
        // Given
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");

        // When / Then
        assertThatThrownBy(file::setUnviable)
                .isInstanceOf(FileException.class)
                .hasMessageContaining("already unviable");
    }

    // -----------------------------------------------------------------------
    // setWaterMark / disableWatermark
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("File can be flagged as watermarked")
    void shouldSetWatermark() {
        // Given
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");

        // When
        file.setWaterMark();

        // Then
        assertThat(file.isHasWatermark()).isTrue();
    }

    @Test
    @DisplayName("Flagging an already watermarked file is refused (the album layer keeps batches idempotent)")
    void shouldThrowWhenSettingWatermarkOnAlreadyWatermarkedFile() {
        // Given
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        file.setWaterMark();

        // When / Then
        assertThatThrownBy(file::setWaterMark)
                .isInstanceOf(FileException.class)
                .hasMessageContaining("already watermarked");
    }

    @Test
    @DisplayName("Watermark flag can be cleared")
    void shouldDisableWatermark() {
        // Given
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        file.setWaterMark();

        // When
        file.disableWatermark();

        // Then
        assertThat(file.isHasWatermark()).isFalse();
    }

    @Test
    @DisplayName("Clearing the watermark flag on a clean file is refused (the album layer keeps batches idempotent)")
    void shouldThrowWhenDisablingWatermarkOnFileWithoutWatermark() {
        // Given
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");

        // When / Then
        assertThatThrownBy(file::disableWatermark)
                .isInstanceOf(FileException.class)
                .hasMessageContaining("not watermarked");
    }
}
