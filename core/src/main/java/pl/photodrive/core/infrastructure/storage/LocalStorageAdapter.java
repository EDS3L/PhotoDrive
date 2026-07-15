package pl.photodrive.core.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import pl.photodrive.core.application.exception.SecurityException;
import pl.photodrive.core.application.port.file.FileStoragePort;
import pl.photodrive.core.infrastructure.exception.StorageException;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;


@Slf4j
@Component
@RequiredArgsConstructor
public class LocalStorageAdapter implements FileStoragePort {

    @Value("${storage.dir}")
    private Path baseDirectory;

    private static final String THUMB_DIR = ".thumbnails";
    // Cache watermarkowanych wersji — jednorazowego użytku (klucz po fileId+wersji loga),
    // można skasować w każdej chwili; NIE wymaga synchronizacji z bazą ani obsługi
    // przy rename/swap/delete (osierocone wpisy są nieszkodliwe).
    private static final String WATERMARK_CACHE_DIR = ".cache/watermark";
    // Cache wariantów dla strony publicznej (A9) — tak samo jednorazowego użytku:
    // klucz niesie fileId + wersję loga + rozmiar, więc osierocone wpisy są nieszkodliwe.
    private static final String PUBLIC_CACHE_DIR = ".cache/public";
    private static final int THUMBNAIL_WIDTH = 600;

    // Pełnowymiarowa kompozycja to dekod całego zdjęcia (~100-200 MB RAM dla 24MP) —
    // na słabym VPS ograniczamy do jednej naraz; miniatury (600px) są tanie i idą bez limitu.
    private static final Semaphore FULL_COMPOSE_PERMIT = new Semaphore(1);

    // Watermark = kafelki po całości (nie da się wykadrować). Rozmiar kafla liczony
    // względem SZEROKOŚCI ZDJĘCIA (spójny na każdej rozdzielczości), na skos, niskie krycie.
    private static final float WATERMARK_ALPHA = 0.20f;
    private static final double WATERMARK_TILE_WIDTH_RATIO = 0.18; // szerokość kafla = % szer. zdjęcia
    private static final double WATERMARK_ANGLE_DEG = -30.0;
    private static final double WATERMARK_GAP_X_RATIO = 0.8; // odstęp poziomy = % szer. kafla
    private static final double WATERMARK_GAP_Y_RATIO = 2.0; // odstęp pionowy = % wys. kafla

    @Override
    public void createPhotographerFolder(String photographerEmail) {
        Path photographerPath = resolveAndValidate(photographerEmail);

        try {
            Files.createDirectories(photographerPath);
            log.info("Created photographer folder: {}", photographerPath);
        } catch (IOException e) {
            throw new StorageException("Failed to create photographer folder", e);
        }
    }

    @Override
    public void createClientAlbum(String albumName, String photographerEmail) {
        Path albumPath = resolveAndValidate(photographerEmail, albumName);

        try {
            Files.createDirectories(albumPath);
            log.info("Created client album: {}/{}", photographerEmail, albumName);
        } catch (IOException e) {
            throw new StorageException("Failed to create client album", e);
        }
    }

    @Override
    public void createAdminAlbum(String albumName) {
        Path albumPath = resolveAndValidate(albumName);

        try {
            Files.createDirectories(albumPath);
            log.info("Created admin album: {}", albumName);
        } catch (IOException e) {
            log.error("Failed to create admin album: {}", albumName, e);
            throw new StorageException("Failed to create admin album", e);
        }
    }

    @Override
    public void saveFile(String path, String fileName, InputStream fileData) throws IOException {
        Path targetDir = resolveAndValidate(path);

        log.info("Saving file in: {}", targetDir);
        if (!Files.isDirectory(targetDir)) {
            throw new StorageException("Target directory does not exist: " + path);
        }

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(baseDirectory, "upload-", ".tmp");
            Files.copy(fileData, tempFile, REPLACE_EXISTING);

            Path targetFile = targetDir.resolve(fileName);

            try {
                Files.move(tempFile, targetFile, ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, targetFile, REPLACE_EXISTING);
            }

            log.info("Saved file: {}/{}", path, fileName);

            createThumbnail(targetDir, fileName, targetFile);

        } catch (IOException e) {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {}
            }
            throw new StorageException("Failed to save file: " + fileName, e);
        }
    }

    private void createThumbnail(Path targetDir, String fileName, Path targetFile) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png")) {
            try {
                Path thumbDir = targetDir.resolve(".thumbnails");
                Files.createDirectories(thumbDir);
                Path thumbFile = thumbDir.resolve(fileName);

                BufferedImage image = ImageIO.read(targetFile.toFile());
                if (image == null) return;

                int finalWidth = THUMBNAIL_WIDTH;
                int finalHeight = Math.max(1,
                        (int) (((double) finalWidth / image.getWidth()) * image.getHeight()));

                // To samo skalowanie schodkowe co przy wariantach publicznych — miniatura z jednego
                // skoku bilinearnego była miękka (patrz scaleTo).
                BufferedImage resizedImage = scaleTo(image, finalWidth, finalHeight);

                String format = lowerName.endsWith(".png") ? "png" : "jpg";

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                if ("jpg".equals(format)) {
                    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
                    if (writers.hasNext()) {
                        ImageWriter writer = writers.next();
                        ImageWriteParam param = writer.getDefaultWriteParam();
                        if (param.canWriteCompressed()) {
                            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                            param.setCompressionQuality(0.7f);
                        }
                        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                            writer.setOutput(ios);
                            writer.write(null, new IIOImage(resizedImage, null, null), param);
                        }
                        writer.dispose();
                    } else {
                        ImageIO.write(resizedImage, format, baos);
                    }
                } else {
                    ImageIO.write(resizedImage, format, baos);
                }
                Files.write(thumbFile, baos.toByteArray());
                log.info("Created thumbnail for {}", fileName);
            } catch (Exception e) {
                log.error("Failed to create thumbnail for {}", fileName, e);
            }
        }
    }

    @Override
    public void deleteFile(String path, String fileName) {
        Path filePath = resolveAndValidate(path, fileName);

        if (!Files.isRegularFile(filePath)) {
            throw new StorageException("File not found: " + path + "/" + fileName);
        }

        try {
            Files.delete(filePath);
            deleteQuietly(filePath.getParent().resolve(THUMB_DIR).resolve(fileName));
        } catch (IOException e) {
            throw new StorageException("Failed to delete file: " + fileName, e);
        }
    }

    @Override
    public void renameFile(String path, String oldName, String newName) {
        Path filePath = resolveAndValidate(path + "/" + oldName);

        if (!Files.isRegularFile(filePath)) {
            throw new StorageException("File not found: " + path);
        }

        try {
            Path targetPath = filePath.resolveSibling(newName);
            if (!targetPath.normalize().startsWith(baseDirectory)) {
                throw new SecurityException("Path traversal attempt in rename: " + newName);
            }

            Files.move(filePath, targetPath);

            Path thumbDir = filePath.getParent().resolve(THUMB_DIR);
            moveIfExists(thumbDir.resolve(oldName), thumbDir.resolve(newName));
        } catch (IOException e) {
            throw new StorageException("Failed to rename file: " + path, e);
        }

    }

    @Override
    public byte[] createZipArchive(String albumPath, List<String> fileNames, Map<String, String> watermarkCacheKeys, byte[] watermarkPng) {
        Path albumDir = resolveAndValidate(albumPath);

        if (!Files.isDirectory(albumDir)) {
            throw new StorageException("Album directory not found: " + albumPath);
        }

        try (var baos = new java.io.ByteArrayOutputStream(); var zos = new ZipOutputStream(baos)) {

            for (String fileName : fileNames) {
                if (fileName == null || fileName.isBlank()) {
                    continue;
                }

                // Pliki oznaczone watermarkiem idą w wersji watermarkowanej (klient nigdy
                // nie dostaje czystego oryginału) — z cache albo komponowane teraz.
                if (watermarkCacheKeys != null && watermarkCacheKeys.containsKey(fileName)) {
                    Resource watermarked = getOrCreateWatermarkedPhoto(albumPath,
                            fileName,
                            watermarkCacheKeys.get(fileName),
                            false,
                            watermarkPng);
                    zos.putNextEntry(new ZipEntry(fileName));
                    try (InputStream in = watermarked.getInputStream()) {
                        in.transferTo(zos);
                    }
                    zos.closeEntry();
                    continue;
                }

                Path filePath = resolveAndValidate(albumPath, fileName);

                if (!Files.isRegularFile(filePath)) {
                    continue;
                }

                ZipEntry entry = new ZipEntry(fileName);
                zos.putNextEntry(entry);
                Files.copy(filePath, zos);
                zos.closeEntry();

                log.debug("Added to ZIP: {}", fileName);
            }

            zos.finish();

            return baos.toByteArray();

        } catch (IOException e) {
            throw new StorageException("Failed to create ZIP archive", e);
        }
    }

    @Override
    public void deleteFolder(String albumPath) {
        Path folderPath = baseDirectory.resolve(albumPath).normalize();
        if (!folderPath.startsWith(baseDirectory)) {
            throw new SecurityException("Path traversal attempt detected: " + albumPath);
        }
        if (!Files.exists(folderPath)) {
            log.warn("Folder not found, cannot delete: {}", folderPath);
            return;
        }

        try {
            Files.walk(folderPath).sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                    log.debug("Deleted: {}", p);
                } catch (IOException e) {
                    throw new StorageException("Failed to delete: " + p, e);
                }
            });

            log.info("Successfully deleted folder: {}", folderPath);
        } catch (IOException e) {
            throw new StorageException("Failed to delete folder: " + folderPath, e);
        }
    }

    @Override
    public Resource getOrCreateWatermarkedPhoto(String albumPath, String fileName, String cacheKey, boolean thumbnail, byte[] watermarkPng) {
        String variant = thumbnail ? "thumb" : "full";
        String extension = fileName.toLowerCase().endsWith(".png") ? "png" : "jpg";
        Path cacheDir = baseDirectory.resolve(WATERMARK_CACHE_DIR).normalize();
        Path cacheFile = cacheDir.resolve(cacheKey + "-" + variant + "." + extension).normalize();
        if (!cacheFile.startsWith(baseDirectory)) {
            throw new SecurityException("Invalid watermark cache key");
        }

        try {
            if (Files.exists(cacheFile)) {
                return new UrlResource(cacheFile.toUri());
            }

            // Miniatura komponowana z istniejącego thumba (600px — tanio); pełna z oryginału.
            Path original = resolveAndValidate(albumPath, fileName);
            Path source = original;
            if (thumbnail) {
                Path thumbSource = original.getParent().resolve(THUMB_DIR).resolve(fileName);
                if (Files.exists(thumbSource)) {
                    source = thumbSource;
                }
            }
            if (!Files.isRegularFile(source)) {
                throw new StorageException("File not found: " + albumPath + "/" + fileName);
            }

            if (!thumbnail) {
                FULL_COMPOSE_PERMIT.acquireUninterruptibly();
            }
            try {
                // Ktoś mógł skomponować, gdy czekaliśmy na semafor.
                if (Files.exists(cacheFile)) {
                    return new UrlResource(cacheFile.toUri());
                }
                composeToCache(source, cacheFile, extension, watermarkPng);
            } finally {
                if (!thumbnail) {
                    FULL_COMPOSE_PERMIT.release();
                }
            }

            return new UrlResource(cacheFile.toUri());
        } catch (IOException e) {
            throw new StorageException("Failed to create watermarked photo: " + fileName, e);
        }
    }

    // Kompozycja kafelków + atomiczny zapis do cache (temp + move), by współbieżny
    // odczyt nie zobaczył połowicznie zapisanego pliku.
    private void composeToCache(Path source, Path cacheFile, String extension, byte[] watermarkPng) throws IOException {
        if (watermarkPng == null || watermarkPng.length == 0) {
            throw new StorageException("No watermark image provided");
        }

        BufferedImage image = ImageIO.read(source.toFile());
        BufferedImage watermarkImage = ImageIO.read(new ByteArrayInputStream(watermarkPng));
        if (image == null || watermarkImage == null) {
            throw new StorageException("Failed to read image for watermark: " + source.getFileName());
        }

        drawWatermark(image, watermarkImage);

        writeToCacheAtomically(image, cacheFile, extension);
        log.info("Composed watermarked variant into cache: {}", cacheFile.getFileName());
    }

    @Override
    public Resource getOrCreatePublicPhoto(String albumPath, String fileName, String cacheKey, int maxDimension, byte[] watermarkPng) {
        String extension = fileName.toLowerCase().endsWith(".png") ? "png" : "jpg";
        Path cacheDir = baseDirectory.resolve(PUBLIC_CACHE_DIR).normalize();
        Path cacheFile = cacheDir.resolve(cacheKey + "." + extension).normalize();
        if (!cacheFile.startsWith(baseDirectory)) {
            throw new SecurityException("Invalid public cache key");
        }

        try {
            if (Files.exists(cacheFile)) {
                return new UrlResource(cacheFile.toUri());
            }

            Path original = resolveAndValidate(albumPath, fileName);
            // Mały wariant można wyprodukować z gotowej miniatury (600px) — dekod pełnego
            // 24MP zdjęcia jest drogi, a kafelki portfolio i tak nie potrzebują więcej.
            Path source = original;
            if (maxDimension <= THUMBNAIL_WIDTH) {
                Path thumbSource = original.getParent().resolve(THUMB_DIR).resolve(fileName);
                if (Files.exists(thumbSource)) {
                    source = thumbSource;
                }
            }
            if (!Files.isRegularFile(source)) {
                throw new StorageException("File not found: " + albumPath + "/" + fileName);
            }

            // Semafor tylko dla drogiej ścieżki (dekod oryginału) — wariant z miniatury jest tani
            // i nie ma powodu ustawiać za nim w kolejce całej siatki portfolio.
            boolean fromOriginal = source.equals(original);
            if (fromOriginal) {
                FULL_COMPOSE_PERMIT.acquireUninterruptibly();
            }
            try {
                // Ktoś mógł zbudować wariant, gdy czekaliśmy na semafor.
                if (Files.exists(cacheFile)) {
                    return new UrlResource(cacheFile.toUri());
                }
                writePublicVariant(source, cacheFile, extension, maxDimension, watermarkPng);
            } finally {
                if (fromOriginal) {
                    FULL_COMPOSE_PERMIT.release();
                }
            }

            return new UrlResource(cacheFile.toUri());
        } catch (IOException e) {
            throw new StorageException("Failed to create public photo variant: " + fileName, e);
        }
    }

    private void writePublicVariant(Path source, Path cacheFile, String extension, int maxDimension, byte[] watermarkPng) throws IOException {
        BufferedImage image = ImageIO.read(source.toFile());
        if (image == null) {
            throw new StorageException("Failed to read image: " + source.getFileName());
        }

        BufferedImage variant = downscale(image, maxDimension);

        if (watermarkPng != null && watermarkPng.length > 0) {
            BufferedImage watermarkImage = ImageIO.read(new ByteArrayInputStream(watermarkPng));
            if (watermarkImage == null) {
                throw new StorageException("Failed to read watermark image");
            }
            // Znak wodny nakładamy PO zmniejszeniu — kafle liczą się względem szerokości obrazu,
            // więc wynik wygląda tak samo jak na oryginale.
            drawWatermark(variant, watermarkImage);
        }

        writeToCacheAtomically(variant, cacheFile, extension);
        log.info("Created public variant ({}px) in cache: {}", maxDimension, cacheFile.getFileName());
    }

    /** Skalowanie po DŁUŻSZYM boku; obraz mniejszy od limitu zostaje bez zmian (nigdy nie powiększamy). */
    private BufferedImage downscale(BufferedImage image, int maxDimension) {
        int longEdge = Math.max(image.getWidth(), image.getHeight());
        if (longEdge <= maxDimension) {
            return image;
        }

        double scale = (double) maxDimension / longEdge;
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));

        return scaleTo(image, width, height);
    }

    /**
     * Skalowanie <b>schodkowe, po połowie</b> — nie jednym skokiem.
     *
     * <p>Powód: {@code drawImage} z BILINEAR próbkuje tylko sąsiedztwo 2×2 piksela źródła, więc
     * skok 4000→800 po prostu WYRZUCA 95% pikseli — wynik jest miękki i aliasowany (drobne wzory
     * zamieniają się w szum). Zmniejszanie o połowę na krok uśrednia pełny obszar i daje ostry
     * obraz. Ostatni krok dochodzi do dokładnego rozmiaru docelowego.
     */
    private BufferedImage scaleTo(BufferedImage image, int targetWidth, int targetHeight) {
        BufferedImage current = image;
        int width = image.getWidth();
        int height = image.getHeight();

        while (width / 2 >= targetWidth && height / 2 >= targetHeight) {
            width = Math.max(targetWidth, width / 2);
            height = Math.max(targetHeight, height / 2);
            current = redraw(current, width, height);
        }

        if (width != targetWidth || height != targetHeight) {
            current = redraw(current, targetWidth, targetHeight);
        }

        return current;
    }

    private BufferedImage redraw(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = target.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(source, 0, 0, width, height, null);
        g2d.dispose();
        return target;
    }

    /** Zapis przez plik tymczasowy + move, żeby współbieżny odczyt nie zobaczył połowicznego pliku. */
    private void writeToCacheAtomically(BufferedImage image, Path cacheFile, String extension) throws IOException {
        Files.createDirectories(cacheFile.getParent());
        Path tempFile = Files.createTempFile(cacheFile.getParent(), "compose-", ".tmp");
        try {
            writeImage(image, extension, tempFile.toFile());
            try {
                Files.move(tempFile, cacheFile, ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, cacheFile, REPLACE_EXISTING);
            }
        } catch (IOException e) {
            deleteQuietly(tempFile);
            throw e;
        }
    }

    @Override
    public void clearWatermarkCache() {
        Path cacheDir = baseDirectory.resolve(WATERMARK_CACHE_DIR).normalize();
        if (!Files.exists(cacheDir)) {
            return;
        }
        try {
            Files.walk(cacheDir).sorted(Comparator.reverseOrder()).forEach(this::deleteQuietly);
            log.info("Cleared watermark cache");
        } catch (IOException e) {
            log.warn("Failed to clear watermark cache", e);
        }
    }

    /**
     * Nakłada znak wodny jako KAFELKI po całej powierzchni (na skos, niskie krycie) —
     * nie da się wykadrować. Rozmiar kafla liczony względem szerokości zdjęcia, więc
     * wygląda tak samo na oryginale i na miniaturze (miniatura powstaje z owatermarkowanego
     * obrazu). Mutuje przekazany obraz w miejscu.
     */
    private void drawWatermark(BufferedImage image, BufferedImage watermarkImage) {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        int tileWidth = Math.max(1, (int) (imageWidth * WATERMARK_TILE_WIDTH_RATIO));
        int tileHeight = Math.max(1,
                (int) ((double) watermarkImage.getHeight() / watermarkImage.getWidth() * tileWidth));

        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, WATERMARK_ALPHA));

        // Obrót całej siatki wokół środka — kafle idą na skos.
        g2d.rotate(Math.toRadians(WATERMARK_ANGLE_DEG), imageWidth / 2.0, imageHeight / 2.0);

        int stepX = tileWidth + (int) (tileWidth * WATERMARK_GAP_X_RATIO);
        int stepY = tileHeight + (int) (tileHeight * WATERMARK_GAP_Y_RATIO);

        // Po obrocie trzeba pokryć obszar większy niż zdjęcie (przekątna z zapasem).
        int reach = (int) Math.hypot(imageWidth, imageHeight);

        int row = 0;
        for (int y = -reach; y < imageHeight + reach; y += stepY, row++) {
            int rowOffset = (row % 2 == 0) ? 0 : stepX / 2; // szachownica
            for (int x = -reach; x < imageWidth + reach; x += stepX) {
                g2d.drawImage(watermarkImage, x + rowOffset, y, tileWidth, tileHeight, null);
            }
        }

        g2d.dispose();
    }

    private void writeImage(BufferedImage image, String format, File outputFile) throws IOException {
        if ("jpg".equalsIgnoreCase(format) || "jpeg".equalsIgnoreCase(format)) {
            saveAsJPEG(image, outputFile, 0.9f);
        } else {
            ImageIO.write(image, format, outputFile);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete {}", path, e);
        }
    }

    /** Przenosi plik, jeśli istnieje (tworzy katalog docelowy) — nie-fatalnie (dane pochodne). */
    private void moveIfExists(Path source, Path target) {
        if (!Files.exists(source)) {
            return;
        }
        try {
            Files.createDirectories(target.getParent());
            Files.move(source, target, REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to move {} to {}", source, target, e);
        }
    }

    @Override
    public void swapFile(String albumPath, String targetPath, String fileName) {
        Path sourceFilePath =  resolveAndValidate(albumPath, fileName);
        Path targetDir =  resolveAndValidate(targetPath);

        if(!Files.isRegularFile(sourceFilePath)) {
            throw new StorageException("File not found: " + sourceFilePath);
        }

        Path targetFilePath = targetDir.resolve(fileName);

        // Backstop dla kolizji nazw: nie nadpisuj istniejącego pliku w albumie docelowym
        // (ATOMIC_MOVE na Linuksie podmieniłby go po cichu). Domena i tak odrzuca kolizję wcześniej.
        if (Files.exists(targetFilePath)) {
            throw new StorageException("Target file already exists, refusing to overwrite: " + targetFilePath);
        }

        log.info("Swapping file {} from {} to {}", fileName, sourceFilePath, targetFilePath);

        try {
            try {
                Files.move(sourceFilePath,targetFilePath,ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                throw new StorageException("Failed to move file from " + sourceFilePath + " to " + targetFilePath, e);
            }

            moveThumbnail(sourceFilePath, targetDir, fileName);

            log.info("Successfully swapped file from {} to {}", sourceFilePath, targetFilePath);
        } catch (IOException e) {
            throw new StorageException("Failed to move file from " + sourceFilePath + " to " + targetFilePath, e);
        }

    }

    // Thumbnail is derived data — once the original has moved, a thumbnail failure must not fail the swap.
    private void moveThumbnail(Path sourceFilePath, Path targetDir, String fileName) {
        Path sourceThumbPath = sourceFilePath.getParent().resolve(".thumbnails").resolve(fileName);
        if (!Files.exists(sourceThumbPath)) {
            return;
        }

        try {
            Path targetThumbDir = targetDir.resolve(".thumbnails");
            Files.createDirectories(targetThumbDir);
            Files.move(sourceThumbPath, targetThumbDir.resolve(fileName), REPLACE_EXISTING);
            log.info("Moved thumbnail for {} to {}", fileName, targetThumbDir);
        } catch (IOException e) {
            log.error("Failed to move thumbnail for {}", fileName, e);
        }
    }

    private void saveAsJPEG(BufferedImage image, File outputFile, float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
            writer.dispose();
        }
    }

    private Path resolveAndValidate(String... pathSegments) {
        Path resolved = baseDirectory;

        for (String segment : pathSegments) {
            if (segment == null || segment.isBlank()) {
                throw new StorageException("Path segment cannot be empty");
            }
            resolved = resolved.resolve(segment);
        }

        Path normalized = resolved.normalize();

        if (!normalized.startsWith(baseDirectory)) {
            throw new SecurityException("Invalid path: path traversal detected");
        }
        return normalized;
    }

}
