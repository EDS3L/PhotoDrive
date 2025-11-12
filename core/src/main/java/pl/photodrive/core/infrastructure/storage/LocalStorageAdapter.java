package pl.photodrive.core.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import pl.photodrive.core.domain.port.StoragePort;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalStorageAdapter implements StoragePort {


    @Value("${storage.dir}")
    private Path DIR;

    @Override
    public void createFolderForPhotograph(String email) {
        try {
            Files.createDirectories(DIR.resolve(email).normalize());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createClientAlbumDir(String name, String photographFolder) {
        Path photographPath = Path.of(DIR + "/" + photographFolder);
        try {
            Files.createDirectories(photographPath.resolve(name).normalize());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createAdminAlbumDir(String name) {
        try {
            Files.createDirectories(DIR.resolve(name).normalize());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void storeByAdmin(String albumName, String fileName, InputStream fileData) {

        Path targetDir = DIR.resolve(albumName).normalize();
        if (!targetDir.startsWith(DIR)) {
            throw new SecurityException("Wrong path!");
        }

        Path tmp = null;
        try {
            tmp = Files.createTempFile(DIR, "upload-", ".tmp");

            Files.copy(fileData,tmp, REPLACE_EXISTING);

            Path target = uniquify(targetDir.resolve(fileName));
            try {
                Files.move(tmp, target, ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, target);
            }
        } catch (IOException e) {
            if (tmp != null) {
                try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            }
            throw new RuntimeException("Failed to save file", e);
        }
    }

    @Override
    public void storeToAlbum(String photographEmail,String albumName, String fileName, InputStream fileData) {
        Path targetDir = DIR.resolve(photographEmail + "/" + albumName).normalize();
        if (!targetDir.startsWith(DIR)) {
            throw new SecurityException("Wrong path!");
        }
        Path tmp = null;
        try {
            tmp = Files.createTempFile(DIR, "upload-", ".tmp");

            Files.copy(fileData,tmp, REPLACE_EXISTING);

            Path target = uniquify(targetDir.resolve(fileName));
            try {
                Files.move(tmp, target, ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, target, REPLACE_EXISTING);
            }
        } catch (IOException e) {
            if (tmp != null) {
                try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            }
            throw new RuntimeException("Failed to save file", e);
        }
    }

    @Override
    public StreamingResponseBody downloadSelectedFilesAsZip(String albumName, List<String> fileNames, String photographEmail) {
        Path albumDir = DIR.resolve(photographEmail).resolve(albumName).normalize();

        if (!albumDir.startsWith(DIR)) throw new SecurityException("Wrong path!");
        if (!Files.isDirectory(albumDir)) throw new RuntimeException("Album not found: " + albumDir);

        return outputStream -> {
            try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
                for (String fn : fileNames) {
                    // prosta sanityzacja
                    if (fn == null || fn.isBlank()) continue;
                    Path filePath = albumDir.resolve(fn).normalize();
                    if (!filePath.startsWith(albumDir)) {
                        log.warn("Skipping suspicious path: {}", filePath);
                        continue;
                    }
                    if (Files.isRegularFile(filePath)) {
                        zos.putNextEntry(new ZipEntry(filePath.getFileName().toString()));
                        Files.copy(filePath, zos);
                        zos.closeEntry();
                    } else {
                        log.warn("Missing or not a file: {}", filePath);
                    }
                }
                zos.finish();
            }
        };

    }

    @Override
    public void delete(String albumId, String fileName) {

    }

    @Override
    public void rename(String albumId, String oldName, String newName) {

    }

    private Path uniquify(Path target) {
        if (!Files.exists(target)) return target;

        String fileName = target.getFileName().toString();
        String name = fileName;
        String ext = "";

        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            name = fileName.substring(0, dot);
            ext  = fileName.substring(dot);
        }

        int i = 1;
        Path candidate;
        do {
            candidate = target.getParent().resolve(name + " (" + i++ + ")" + ext);
        } while (Files.exists(candidate));

        return candidate;
    }
}
