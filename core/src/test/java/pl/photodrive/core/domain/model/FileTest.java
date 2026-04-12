package pl.photodrive.core.domain.model;

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
    void shouldThrowWhenSizeIsZero() {
        assertThatThrownBy(() -> File.create(new FileName("photo.jpg"), 0L, "image/jpeg"))
                .isInstanceOf(FileException.class)
                .hasMessageContaining("negative");
    }

    @Test
    void shouldThrowWhenSizeIsNegative() {
        assertThatThrownBy(() -> File.create(new FileName("photo.jpg"), -1L, "image/jpeg"))
                .isInstanceOf(FileException.class);
    }

    // -----------------------------------------------------------------------
    // Constructor validation
    // -----------------------------------------------------------------------

    @Test
    void shouldThrowWhenContentTypeIsNull() {
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
    void shouldThrowWhenUploadedAtIsInFuture() {
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
    void shouldRenameFileSuccessfully() {
        // Given
        File file = File.create(new FileName("old.jpg"), 100L, "image/jpeg");

        // When
        file.rename(new FileName("new.jpg"));

        // Then
        assertThat(file.getFileName().value()).isEqualTo("new.jpg");
    }

    @Test
    void shouldThrowWhenRenamingWithNull() {
        File file = File.create(new FileName("old.jpg"), 100L, "image/jpeg");
        assertThatThrownBy(() -> file.rename(null))
                .isInstanceOf(FileException.class);
    }

    // -----------------------------------------------------------------------
    // setViable / setUnviable
    // -----------------------------------------------------------------------

    @Test
    void shouldSetFileVisible() {
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        file.setViable();
        assertThat(file.isVisible()).isTrue();
    }

    @Test
    void shouldThrowWhenSettingVisibleOnAlreadyVisibleFile() {
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        file.setViable();
        assertThatThrownBy(file::setViable)
                .isInstanceOf(FileException.class)
                .hasMessageContaining("already viable");
    }

    @Test
    void shouldSetFileUnviable() {
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        file.setViable();
        file.setUnviable();
        assertThat(file.isVisible()).isFalse();
    }

    @Test
    void shouldThrowWhenSettingUnviableOnAlreadyUnviableFile() {
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        assertThatThrownBy(file::setUnviable)
                .isInstanceOf(FileException.class)
                .hasMessageContaining("already unviable");
    }

    // -----------------------------------------------------------------------
    // setWaterMark / disableWatermark
    // -----------------------------------------------------------------------

    @Test
    void shouldSetWatermark() {
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        file.setWaterMark();
        assertThat(file.isHasWatermark()).isTrue();
    }

    @Test
    void shouldThrowWhenSettingWatermarkOnAlreadyWatermarkedFile() {
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        file.setWaterMark();
        assertThatThrownBy(file::setWaterMark)
                .isInstanceOf(FileException.class)
                .hasMessageContaining("already watermarked");
    }

    @Test
    void shouldDisableWatermark() {
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        file.setWaterMark();
        file.disableWatermark();
        assertThat(file.isHasWatermark()).isFalse();
    }

    @Test
    void shouldThrowWhenDisablingWatermarkOnFileWithoutWatermark() {
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        assertThatThrownBy(file::disableWatermark)
                .isInstanceOf(FileException.class)
                .hasMessageContaining("not watermarked");
    }
}
