package pl.photodrive.core.infrastructure.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Swap moves the original byte for byte, without re-encoding it")
    void shouldMoveOriginalFileUnchangedWhenSwapping() throws IOException {
        // Given
        byte[] originalBytes = {12, 34, 56, 78, 90};
        Files.write(sourceAlbum.resolve("photo.jpg"), originalBytes);

        // When
        adapter.swapFile("source-album", "target-album", "photo.jpg");

        // Then
        assertFalse(Files.exists(sourceAlbum.resolve("photo.jpg")));
        assertArrayEquals(originalBytes, Files.readAllBytes(targetAlbum.resolve("photo.jpg")));
    }

    @Test
    @DisplayName("Swap moves the thumbnail too, so the preview keeps its quality")
    void shouldMoveThumbnailToTargetAlbumWhenSwapping() throws IOException {
        // Given
        byte[] thumbnailBytes = {1, 2, 3};
        Files.write(sourceAlbum.resolve("photo.jpg"), new byte[]{9, 9, 9});
        Path sourceThumbDir = Files.createDirectories(sourceAlbum.resolve(".thumbnails"));
        Files.write(sourceThumbDir.resolve("photo.jpg"), thumbnailBytes);

        adapter.swapFile("source-album", "target-album", "photo.jpg");

        // When
        Path targetThumb = targetAlbum.resolve(".thumbnails").resolve("photo.jpg");

        // Then
        assertFalse(Files.exists(sourceThumbDir.resolve("photo.jpg")));
        assertArrayEquals(thumbnailBytes, Files.readAllBytes(targetThumb));
    }

    @Test
    @DisplayName("Swap succeeds even when no thumbnail exists, because a thumbnail is derived data")
    void shouldSwapFileWithoutThumbnailWhenThumbnailDoesNotExist() throws IOException {
        // Given
        Files.write(sourceAlbum.resolve("photo.webp"), new byte[]{7, 7});

        // When
        adapter.swapFile("source-album", "target-album", "photo.webp");

        // Then
        assertTrue(Files.exists(targetAlbum.resolve("photo.webp")));
        assertFalse(Files.exists(targetAlbum.resolve(".thumbnails").resolve("photo.webp")));
    }

    @Test
    @DisplayName("Swap replaces a stale thumbnail in the target album")
    void shouldReplaceExistingThumbnailInTargetAlbumWhenSwapping() throws IOException {
        // Given
        byte[] freshThumbnail = {4, 4, 4};
        Files.write(sourceAlbum.resolve("photo.jpg"), new byte[]{9});
        Path sourceThumbDir = Files.createDirectories(sourceAlbum.resolve(".thumbnails"));
        Files.write(sourceThumbDir.resolve("photo.jpg"), freshThumbnail);

        Path targetThumbDir = Files.createDirectories(targetAlbum.resolve(".thumbnails"));
        Files.write(targetThumbDir.resolve("photo.jpg"), new byte[]{0});

        // When
        adapter.swapFile("source-album", "target-album", "photo.jpg");

        // Then
        assertArrayEquals(freshThumbnail, Files.readAllBytes(targetThumbDir.resolve("photo.jpg")));
    }

    @Test
    @DisplayName("Swapping a file that is not on disk fails loudly")
    void shouldThrowExceptionWhenSwappedFileDoesNotExist() {
        // When / Then
        assertThrows(StorageException.class,
                () -> adapter.swapFile("source-album", "target-album", "missing.jpg"));
    }

    // ---------- Watermark: composed on the fly, cached by key (fileId-version) ----------

    @Test
    @DisplayName("Watermark is composed into a cache entry and the original stays untouched")
    void shouldComposeWatermarkedPhotoWithoutTouchingOriginal() throws IOException {
        // Given
        Path original = sourceAlbum.resolve("photo.jpg");
        writeImage(original, 200, 150);
        byte[] originalBytes = Files.readAllBytes(original);

        var resource = adapter.getOrCreateWatermarkedPhoto("source-album", "photo.jpg", "file1-100", false, watermarkPng());

        // The original is UNTOUCHED, the result lands in the cache and keeps the original size
        assertArrayEquals(originalBytes, Files.readAllBytes(original));

        // When
        Path cacheFile = baseDirectory.resolve(".cache/watermark/file1-100-full.jpg");

        // Then
        assertTrue(Files.exists(cacheFile));
        assertEquals(200, readImage(resource).getWidth());
    }

    @Test
    @DisplayName("Second request reuses the cached watermarked photo instead of composing it again")
    void shouldServeCachedWatermarkOnSecondCall() throws IOException {
        // Given
        writeImage(sourceAlbum.resolve("photo.jpg"), 200, 150);
        adapter.getOrCreateWatermarkedPhoto("source-album", "photo.jpg", "file1-100", false, watermarkPng());

        // Swap the cache content for a marker: the second read must hit the cache, not recompose
        Path cacheFile = baseDirectory.resolve(".cache/watermark/file1-100-full.jpg");
        byte[] marker = {7, 7, 7};
        Files.write(cacheFile, marker);

        // When
        var resource = adapter.getOrCreateWatermarkedPhoto("source-album", "photo.jpg", "file1-100", false, watermarkPng());

        // Then
        assertArrayEquals(marker, readBytes(resource));
    }

    @Test
    @DisplayName("Thumbnail variant is composed from the small thumbnail, which is cheap on a weak server")
    void shouldComposeThumbVariantFromThumbnailSource() throws IOException {
        // Given
        writeImage(sourceAlbum.resolve("photo.jpg"), 200, 150);
        Path thumbDir = Files.createDirectories(sourceAlbum.resolve(".thumbnails"));
        writeImage(thumbDir.resolve("photo.jpg"), 100, 75);

        var resource = adapter.getOrCreateWatermarkedPhoto("source-album", "photo.jpg", "file1-100", true, watermarkPng());

        // When
        // The thumb variant is composed from the thumbnail (100px), not from the original (200px)
        assertEquals(100, readImage(resource).getWidth());

        // Then
        assertTrue(Files.exists(baseDirectory.resolve(".cache/watermark/file1-100-thumb.jpg")));
    }

    @Test
    @DisplayName("Missing thumbnail falls back to the original as the composition source")
    void shouldFallBackToOriginalWhenThumbnailMissingForThumbVariant() throws IOException {
        // Given
        writeImage(sourceAlbum.resolve("photo.jpg"), 200, 150);

        // When
        var resource = adapter.getOrCreateWatermarkedPhoto("source-album", "photo.jpg", "file1-100", true, watermarkPng());

        // Then
        assertEquals(200, readImage(resource).getWidth());
    }

    @Test
    @DisplayName("Composing a watermark for a missing file fails instead of serving nothing")
    void shouldThrowWhenComposingMissingFile() throws IOException {
        // Given
        byte[] watermark = watermarkPng();

        // When / Then
        assertThrows(StorageException.class,
                () -> adapter.getOrCreateWatermarkedPhoto("source-album", "missing.jpg", "x-1", false, watermark));
    }

    @Test
    @DisplayName("ZIP contains the watermarked version for a flagged photo")
    void shouldZipWatermarkedVersionForFlaggedFile() throws IOException {
        // Given
        Path original = sourceAlbum.resolve("photo.jpg");
        writeImage(original, 200, 150);
        byte[] originalBytes = Files.readAllBytes(original);

        byte[] zip = adapter.createZipArchive("source-album",
                java.util.List.of("photo.jpg"),
                java.util.Map.of("photo.jpg", "file1-100"),
                watermarkPng());

        // When
        byte[] entry = readZipEntry(zip, "photo.jpg");

        // Then
        assertNotNull(entry);
        // The entry is the composed version (not the original) and matches the cached bytes
        assertFalse(java.util.Arrays.equals(originalBytes, entry));
        Path cacheFile = baseDirectory.resolve(".cache/watermark/file1-100-full.jpg");
        assertArrayEquals(Files.readAllBytes(cacheFile), entry);
    }

    @Test
    @DisplayName("ZIP contains clean originals when no watermark was requested")
    void shouldZipOriginalWhenNoWatermarkKeys() throws IOException {
        // Given
        byte[] originalBytes = {2, 2, 2};
        Files.write(sourceAlbum.resolve("photo.jpg"), originalBytes);

        // When
        byte[] zip = adapter.createZipArchive("source-album",
                java.util.List.of("photo.jpg"),
                java.util.Map.of(),
                null);

        // Then
        assertArrayEquals(originalBytes, readZipEntry(zip, "photo.jpg"));
    }

    @Test
    @DisplayName("Clearing the cache removes every rendered watermark, so a new logo takes effect")
    void shouldClearWatermarkCache() throws IOException {
        // Given
        writeImage(sourceAlbum.resolve("photo.jpg"), 200, 150);

        // When
        adapter.getOrCreateWatermarkedPhoto("source-album", "photo.jpg", "file1-100", false, watermarkPng());

        // Then
        assertTrue(Files.exists(baseDirectory.resolve(".cache/watermark/file1-100-full.jpg")));

        adapter.clearWatermarkCache();

        assertFalse(Files.exists(baseDirectory.resolve(".cache/watermark/file1-100-full.jpg")));
    }

    // ---------- public variants (A9) ----------

    @Test
    @DisplayName("Public variant is scaled by its LONGEST edge, so a portrait crop cannot smuggle out full resolution")
    void shouldScalePublicVariantByLongestEdge() throws IOException {
        // Given - a tall photo: capping the WIDTH alone would still leave 2x the height
        writeImage(sourceAlbum.resolve("photo.jpg"), 1000, 2000);

        // When
        var resource = adapter.getOrCreatePublicPhoto("source-album", "photo.jpg", "file1-500", 500);

        // Then - the long edge lands exactly on the limit and the aspect ratio survives
        BufferedImage variant = readImage(resource);
        assertEquals(500, variant.getHeight());
        assertEquals(250, variant.getWidth());
    }

    @Test
    @DisplayName("Public variant lands in the cache and the original on disk stays untouched")
    void shouldCachePublicVariantWithoutTouchingOriginal() throws IOException {
        // Given
        Path original = sourceAlbum.resolve("photo.jpg");
        writeImage(original, 2000, 1500);
        byte[] originalBytes = Files.readAllBytes(original);

        // When
        adapter.getOrCreatePublicPhoto("source-album", "photo.jpg", "file1-800", 800);

        // Then
        assertTrue(Files.exists(baseDirectory.resolve(".cache/public/file1-800.jpg")));
        assertArrayEquals(originalBytes, Files.readAllBytes(original));
    }

    @Test
    @DisplayName("A photo smaller than the limit is served as it is, never blown up")
    void shouldNotUpscalePublicVariantSmallerThanTheLimit() throws IOException {
        // Given
        writeImage(sourceAlbum.resolve("photo.jpg"), 200, 150);

        // When
        var resource = adapter.getOrCreatePublicPhoto("source-album", "photo.jpg", "file1-2560", 2560);

        // Then
        BufferedImage variant = readImage(resource);
        assertEquals(200, variant.getWidth());
        assertEquals(150, variant.getHeight());
    }

    @Test
    @DisplayName("Second request reuses the cached public variant instead of re-encoding the photo")
    void shouldServeCachedPublicVariantOnSecondCall() throws IOException {
        // Given
        writeImage(sourceAlbum.resolve("photo.jpg"), 2000, 1500);
        adapter.getOrCreatePublicPhoto("source-album", "photo.jpg", "file1-800", 800);

        // Swap the cache content for a marker: the second read must hit the cache, not rebuild
        byte[] marker = {7, 7, 7};
        Files.write(baseDirectory.resolve(".cache/public/file1-800.jpg"), marker);

        // When
        var resource = adapter.getOrCreatePublicPhoto("source-album", "photo.jpg", "file1-800", 800);

        // Then
        assertArrayEquals(marker, readBytes(resource));
    }

    @Test
    @DisplayName("A small public variant is built from the thumbnail, so the portfolio grid never decodes originals")
    void shouldBuildSmallPublicVariantFromThumbnail() throws IOException {
        // Given - the thumbnail is deliberately narrower than the original
        writeImage(sourceAlbum.resolve("photo.jpg"), 2000, 1500);
        Path thumbDir = Files.createDirectories(sourceAlbum.resolve(".thumbnails"));
        writeImage(thumbDir.resolve("photo.jpg"), 100, 75);

        // When
        var resource = adapter.getOrCreatePublicPhoto("source-album", "photo.jpg", "file1-600", 600);

        // Then - 100px proves the thumbnail was the source (the original would have given 600)
        assertEquals(100, readImage(resource).getWidth());
    }

    @Test
    @DisplayName("Downscaling averages the pixels it drops, so fine detail does not collapse into noise")
    void shouldDownscalePublicVariantWithoutAliasing() throws IOException {
        // Given - fine vertical stripes, 1 black column in every 3. Their true average is 170.
        // Scaled in ONE bilinear jump the sampler only looks at a 2x2 neighbourhood of the source
        // and throws the other 250 pixels away, so the result depends on WHERE the sample lands -
        // the stripes turn into banding. Averaging every dropped pixel (halving step by step) must
        // land on the real average instead.
        // PNG on purpose: JPEG would smear the pattern by itself and the test would measure nothing.
        writeStripes(sourceAlbum.resolve("photo.png"), 1600, 1600);

        // When
        var resource = adapter.getOrCreatePublicPhoto("source-album", "photo.png", "file1-100", 100);

        // Then
        BufferedImage variant = readImage(resource);
        int worstDeviation = 0;
        for (int x = 10; x < 90; x++) {
            for (int y = 10; y < 90; y++) {
                int blue = variant.getRGB(x, y) & 0xFF; // obraz jest szary, jeden kanał wystarczy
                worstDeviation = Math.max(worstDeviation, Math.abs(blue - 170));
            }
        }
        assertTrue(worstDeviation < 25,
                "Detail turned into banding instead of being averaged: worst deviation " + worstDeviation);
    }

    @Test
    @DisplayName("Asking for a public variant of a missing file fails instead of serving nothing")
    void shouldThrowWhenPublicVariantSourceIsMissing() {
        // When / Then
        assertThrows(StorageException.class,
                () -> adapter.getOrCreatePublicPhoto("source-album", "missing.jpg", "x-800", 800));
    }

    // ---------- helpers ----------

    /**
     * Pionowe pasy o okresie 3 (1 czarna kolumna na 3) w bezstratnym PNG — średnia = 170.
     * Szachownica się tu nie nadaje: KAŻDE okno 2×2 zawiera dokładnie połowę czerni, więc nawet
     * najgorsze skalowanie trafia w idealną szarość i test nic nie mierzy (sprawdzone).
     */
    private void writeStripes(Path path, int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            int color = (x % 3 == 0) ? 0x000000 : 0xFFFFFF;
            for (int y = 0; y < height; y++) {
                img.setRGB(x, y, color);
            }
        }
        ImageIO.write(img, "png", path.toFile());
    }

    private void writeImage(Path path, int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, width, height);
        g.dispose();
        String fmt = path.getFileName().toString().toLowerCase().endsWith(".png") ? "png" : "jpg";
        ImageIO.write(img, fmt, path.toFile());
    }

    // Windows: an unclosed stream locks the file and breaks @TempDir cleanup.
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

    /** Small semi-transparent PNG logo as bytes, the way the adapter receives it from the database. */
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
