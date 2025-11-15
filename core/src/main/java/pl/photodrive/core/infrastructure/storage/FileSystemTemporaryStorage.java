package pl.photodrive.core.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.photodrive.core.application.port.TemporaryStoragePort;
import pl.photodrive.core.infrastructure.exception.StorageException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileSystemTemporaryStorage implements TemporaryStoragePort {

    private final Path tempDirectory;

    public FileSystemTemporaryStorage(@Value("${storage.temp.path}") String tempPath) {
        this.tempDirectory = Paths.get(tempPath);
        try {
            Files.createDirectories(tempDirectory);
        } catch (IOException e) {
            throw new StorageException("Cannot create temp directory", e);
        }
    }

    @Override
    public boolean exists(String tempId) {
        Path tempFile = tempDirectory.resolve(tempId);
        return Files.exists(tempFile) && Files.isRegularFile(tempFile);
    }

    @Override
    public String saveTemporary(InputStream data) throws IOException {
        String tempId = UUID.randomUUID().toString();
        Path tempFile = tempDirectory.resolve(tempId);
        Files.copy(data, tempFile, StandardCopyOption.REPLACE_EXISTING);
        return tempId;
    }

    @Override
    public InputStream getFile(String fileName) throws IOException {
        Path tempFile = tempDirectory.resolve(fileName);
        if (!Files.exists(tempFile)) {
            throw new StorageException("Temporary file not found: " + fileName);
        }
        return Files.newInputStream(tempFile);
    }

    @Override
    public void delete(String tempId) {
        try {
            Path tempFile = tempDirectory.resolve(tempId);
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            log.warn("Failed to delete temporary file: {}", tempId, e);
        }
    }
}