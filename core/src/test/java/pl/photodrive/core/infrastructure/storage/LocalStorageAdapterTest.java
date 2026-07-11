package pl.photodrive.core.infrastructure.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import pl.photodrive.core.infrastructure.exception.StorageException;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

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

    // ---------- Watermark: kompozycja w locie + cache po kluczu (fileId-wersja) ----------

    @Test
    void shouldComposeWatermarkedPhotoWithoutTouchingOriginal() throws IOException {
        Path original = sourceAlbum.resolve("photo.jpg");
        writeImage(original, 200, 150);
        byte[] originalBytes = Files.readAllBytes(original);

        var resource = adapter.getOrCreateWatermarkedPhoto("source-album", "photo.jpg", "file1-100", false, watermarkPng());

        // Oryginał NIETKNIĘTY, wynik w cache, wymiary jak oryginał
        assertArrayEquals(originalBytes, Files.readAllBytes(original));
        Path cacheFile = baseDirectory.resolve(".cache/watermark/file1-100-full.jpg");
        assertTrue(Files.exists(cacheFile));
        assertEquals(200, readImage(resource).getWidth());
    }

    @Test
    void shouldServeCachedWatermarkOnSecondCall() throws IOException {
        writeImage(sourceAlbum.resolve("photo.jpg"), 200, 150);
        adapter.getOrCreateWatermarkedPhoto("source-album", "photo.jpg", "file1-100", false, watermarkPng());

        // Podmieniamy zawartość cache markerem — drugi odczyt musi trafić w cache, nie komponować
        Path cacheFile = baseDirectory.resolve(".cache/watermark/file1-100-full.jpg");
        byte[] marker = {7, 7, 7};
        Files.write(cacheFile, marker);

        var resource = adapter.getOrCreateWatermarkedPhoto("source-album", "photo.jpg", "file1-100", false, watermarkPng());

        assertArrayEquals(marker, readBytes(resource));
    }

    @Test
    void shouldComposeThumbVariantFromThumbnailSource() throws IOException {
        writeImage(sourceAlbum.resolve("photo.jpg"), 200, 150);
        Path thumbDir = Files.createDirectories(sourceAlbum.resolve(".thumbnails"));
        writeImage(thumbDir.resolve("photo.jpg"), 100, 75);

        var resource = adapter.getOrCreateWatermarkedPhoto("source-album", "photo.jpg", "file1-100", true, watermarkPng());

        // Wariant thumb komponowany z miniatury (100px), nie z oryginału (200px)
        assertEquals(100, readImage(resource).getWidth());
        assertTrue(Files.exists(baseDirectory.resolve(".cache/watermark/file1-100-thumb.jpg")));
    }

    @Test
    void shouldFallBackToOriginalWhenThumbnailMissingForThumbVariant() throws IOException {
        writeImage(sourceAlbum.resolve("photo.jpg"), 200, 150);

        var resource = adapter.getOrCreateWatermarkedPhoto("source-album", "photo.jpg", "file1-100", true, watermarkPng());

        assertEquals(200, readImage(resource).getWidth());
    }

    @Test
    void shouldThrowWhenComposingMissingFile() throws IOException {
        byte[] watermark = watermarkPng();
        assertThrows(StorageException.class,
                () -> adapter.getOrCreateWatermarkedPhoto("source-album", "missing.jpg", "x-1", false, watermark));
    }

    @Test
    void shouldZipWatermarkedVersionForFlaggedFile() throws IOException {
        Path original = sourceAlbum.resolve("photo.jpg");
        writeImage(original, 200, 150);
        byte[] originalBytes = Files.readAllBytes(original);

        byte[] zip = adapter.createZipArchive("source-album",
                java.util.List.of("photo.jpg"),
                java.util.Map.of("photo.jpg", "file1-100"),
                watermarkPng());

        byte[] entry = readZipEntry(zip, "photo.jpg");
        assertNotNull(entry);
        // Wpis to skomponowana wersja (≠ oryginał), zgodna z zawartością cache
        assertFalse(java.util.Arrays.equals(originalBytes, entry));
        Path cacheFile = baseDirectory.resolve(".cache/watermark/file1-100-full.jpg");
        assertArrayEquals(Files.readAllBytes(cacheFile), entry);
    }

    @Test
    void shouldZipOriginalWhenNoWatermarkKeys() throws IOException {
        byte[] originalBytes = {2, 2, 2};
        Files.write(sourceAlbum.resolve("photo.jpg"), originalBytes);

        byte[] zip = adapter.createZipArchive("source-album",
                java.util.List.of("photo.jpg"),
                java.util.Map.of(),
                null);

        assertArrayEquals(originalBytes, readZipEntry(zip, "photo.jpg"));
    }

    @Test
    void shouldClearWatermarkCache() throws IOException {
        writeImage(sourceAlbum.resolve("photo.jpg"), 200, 150);
        adapter.getOrCreateWatermarkedPhoto("source-album", "photo.jpg", "file1-100", false, watermarkPng());
        assertTrue(Files.exists(baseDirectory.resolve(".cache/watermark/file1-100-full.jpg")));

        adapter.clearWatermarkCache();

        assertFalse(Files.exists(baseDirectory.resolve(".cache/watermark/file1-100-full.jpg")));
    }

    // ---------- helpers ----------

    private void writeImage(Path path, int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, width, height);
        g.dispose();
        String fmt = path.getFileName().toString().toLowerCase().endsWith(".png") ? "png" : "jpg";
        ImageIO.write(img, fmt, path.toFile());
    }

    // Windows: niezamknięty strumień = plik nie do skasowania → wywala sprzątanie @TempDir.
    private BufferedImage readImage(org.springframework.core.io.Resource resource) throws IOException {
        try (var in = resource.getInputStream()) {
            return ImageIO.read(in);
        }
    }

    private byte[] readBytes(org.springframework.core.io.Resource resource) throws IOException {
        try (var in = resource.getInputStream()) {
            return in.readAllBytes();
        }
    }

    /** Małe półprzezroczyste logo PNG jako bajty (tak logo trafia z bazy do adaptera). */
    private byte[] watermarkPng() throws IOException {
        BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(255, 255, 255, 180));
        g.fillRect(0, 0, 20, 20);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private byte[] readZipEntry(byte[] zip, String entryName) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(entryName)) {
                    return zis.readAllBytes();
                }
            }
        }
        return null;
    }
}
