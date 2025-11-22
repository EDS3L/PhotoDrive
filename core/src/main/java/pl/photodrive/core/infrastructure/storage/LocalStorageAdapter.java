package pl.photodrive.core.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.photodrive.core.application.exception.SecurityException;
import pl.photodrive.core.application.port.file.FileStoragePort;
import pl.photodrive.core.infrastructure.exception.StorageException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
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
    public InputStream getFile(String path, String fileName) {
        Path filePath = resolveAndValidate(path, fileName);

        if (!Files.isRegularFile(filePath)) {
            throw new StorageException("File not found: " + path + "/" + fileName);
        }

        try {
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            throw new StorageException("Failed to open file: " + fileName, e);
        }
    }
    @Override
    public void deleteFile(String path, String fileName) {

    }

    @Override
    public void renameFile(String path, String oldName, String newName) {

    }

    @Override
    public byte[] createZipArchive(String albumPath, List<String> fileNames) {
        Path albumDir = resolveAndValidate(albumPath);

        if (!Files.isDirectory(albumDir)) {
            throw new StorageException("Album directory not found: " + albumPath);
        }

        try (var baos = new java.io.ByteArrayOutputStream();
             var zos = new ZipOutputStream(baos)) {

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
    public void deleteFolder(String albumName, String photographerEmail) {
        Path folderPath = resolveAndValidate(photographerEmail, albumName);

        if (!Files.exists(folderPath)) {
            log.warn("Folder not found, cannot delete: {}", folderPath);
            return;
        }

        try {
            Files.walk(folderPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
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
