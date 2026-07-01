package pl.photodrive.core.infrastructure.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import pl.photodrive.core.infrastructure.exception.StorageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageAdapterTest {

    @TempDir
    Path baseDirectory;

    private LocalStorageAdapter adapter;

    private Path sourceAlbum;
    private Path targetAlbum;

    @BeforeEach
    void setUp() throws IOException {
        adapter = new LocalStorageAdapter();
        ReflectionTestUtils.setField(adapter, "baseDirectory", baseDirectory);

        sourceAlbum = Files.createDirectories(baseDirectory.resolve("source-album"));
        targetAlbum = Files.createDirectories(baseDirectory.resolve("target-album"));
    }

    @Test
    void shouldMoveOriginalFileUnchangedWhenSwapping() throws IOException {
        byte[] originalBytes = {12, 34, 56, 78, 90};
        Files.write(sourceAlbum.resolve("photo.jpg"), originalBytes);

        adapter.swapFile("source-album", "target-album", "photo.jpg");

        assertFalse(Files.exists(sourceAlbum.resolve("photo.jpg")));
        assertArrayEquals(originalBytes, Files.readAllBytes(targetAlbum.resolve("photo.jpg")));
    }

    @Test
    void shouldMoveThumbnailToTargetAlbumWhenSwapping() throws IOException {
        byte[] thumbnailBytes = {1, 2, 3};
        Files.write(sourceAlbum.resolve("photo.jpg"), new byte[]{9, 9, 9});
        Path sourceThumbDir = Files.createDirectories(sourceAlbum.resolve(".thumbnails"));
        Files.write(sourceThumbDir.resolve("photo.jpg"), thumbnailBytes);

        adapter.swapFile("source-album", "target-album", "photo.jpg");

        Path targetThumb = targetAlbum.resolve(".thumbnails").resolve("photo.jpg");
        assertFalse(Files.exists(sourceThumbDir.resolve("photo.jpg")));
        assertArrayEquals(thumbnailBytes, Files.readAllBytes(targetThumb));
    }

    @Test
    void shouldSwapFileWithoutThumbnailWhenThumbnailDoesNotExist() throws IOException {
        Files.write(sourceAlbum.resolve("photo.webp"), new byte[]{7, 7});

        adapter.swapFile("source-album", "target-album", "photo.webp");

        assertTrue(Files.exists(targetAlbum.resolve("photo.webp")));
        assertFalse(Files.exists(targetAlbum.resolve(".thumbnails").resolve("photo.webp")));
    }

    @Test
    void shouldReplaceExistingThumbnailInTargetAlbumWhenSwapping() throws IOException {
        byte[] freshThumbnail = {4, 4, 4};
        Files.write(sourceAlbum.resolve("photo.jpg"), new byte[]{9});
        Path sourceThumbDir = Files.createDirectories(sourceAlbum.resolve(".thumbnails"));
        Files.write(sourceThumbDir.resolve("photo.jpg"), freshThumbnail);

        Path targetThumbDir = Files.createDirectories(targetAlbum.resolve(".thumbnails"));
        Files.write(targetThumbDir.resolve("photo.jpg"), new byte[]{0});

        adapter.swapFile("source-album", "target-album", "photo.jpg");

        assertArrayEquals(freshThumbnail, Files.readAllBytes(targetThumbDir.resolve("photo.jpg")));
    }

    @Test
    void shouldThrowExceptionWhenSwappedFileDoesNotExist() {
        assertThrows(StorageException.class,
                () -> adapter.swapFile("source-album", "target-album", "missing.jpg"));
    }
}
