package pl.photodrive.core.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.photodrive.core.application.exception.SecurityException;
import pl.photodrive.core.application.port.file.FileStoragePort;
import pl.photodrive.core.infrastructure.exception.StorageException;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
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

        } catch (IOException e) {
            if (tempFile != null) {

                Files.deleteIfExists(tempFile);

            }
            throw new StorageException("Failed to save file: " + fileName, e);
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

            Files.move(filePath, targetPath);
        } catch (IOException e) {
            throw new StorageException("Failed to rename file: " + path, e);
        }

    }

    @Override
    public byte[] createZipArchive(String albumPath, List<String> fileNames) {
        Path albumDir = resolveAndValidate(albumPath);

        if (!Files.isDirectory(albumDir)) {
            throw new StorageException("Album directory not found: " + albumPath);
        }

        try (var baos = new java.io.ByteArrayOutputStream(); var zos = new ZipOutputStream(baos)) {

            for (String fileName : fileNames) {
                if (fileName == null || fileName.isBlank()) {
                    continue;
                }

                Path filePath = resolveAndValidate(albumPath, fileName);

                if (!Files.isRegularFile(filePath)) {
                    continue;
                }

                ZipEntry entry = new ZipEntry(filePath.getFileName().toString());
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
    public void addWatermark(String path) {
        String WATERMARK_PATH = baseDirectory + "/" + "watermark" + "/" + "watermark.png";

        File sourceFile = new File(baseDirectory + "/" + path);
        log.info("Adding source: {}", sourceFile.getPath());
        File watermarkFile = new File(WATERMARK_PATH);
        log.info("Adding watermark: {}", watermarkFile.getPath());

        if (!sourceFile.exists() || !watermarkFile.exists()) {
            throw new StorageException("File not found: " + path);
        }

        float alpha = 0.9f;

        try {
            BufferedImage image = ImageIO.read(sourceFile);
            BufferedImage watermarkImage = ImageIO.read(watermarkFile);

            String originalFormat = getImageFormat(sourceFile);

            Graphics2D g2d = image.createGraphics();

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            double scaleFactor = 3.0;

            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            int watermarkWidth = (int) (watermarkImage.getWidth() * scaleFactor);
            int watermarkHeight = (int) (watermarkImage.getHeight() * scaleFactor);

            int marginX = (int) (imageWidth * 0.10);
            int marginY = (int) (imageHeight * 0.10);
            int x = imageWidth - watermarkWidth - marginX;
            int y = imageHeight - watermarkHeight - marginY;

            g2d.drawImage(watermarkImage, x, y, watermarkWidth, watermarkHeight, null);
            g2d.dispose();

            File outputFile = new File(baseDirectory + "/" + path);

            if ("jpg".equalsIgnoreCase(originalFormat) || "jpeg".equalsIgnoreCase(originalFormat)) {
                saveAsJPEG(image, outputFile, 0.9f);
            } else {
                ImageIO.write(image, originalFormat, outputFile);
            }

            log.info("Successfully added watermark: {}", path);
        } catch (IOException e) {
            throw new StorageException("Failed to add watermark to file", e);
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

        log.info("Swapping file {} from {} to {}", fileName, sourceFilePath, targetFilePath);

        try {
            try {
                Files.move(sourceFilePath,targetFilePath,ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                throw new StorageException("Failed to move file from " + sourceFilePath + " to " + targetFilePath, e);
            }

            log.info("Successfully swapped file from {} to {}", sourceFilePath, targetFilePath);
        } catch (IOException e) {
            throw new StorageException("Failed to move file from " + sourceFilePath + " to " + targetFilePath, e);
        }



    }

    private String getImageFormat(File file) throws IOException {
        ImageInputStream iis = ImageIO.createImageInputStream(file);
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
        if (readers.hasNext()) {
            ImageReader reader = readers.next();
            String format = reader.getFormatName();
            iis.close();
            return format;
        }
        iis.close();
        return "png";
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
