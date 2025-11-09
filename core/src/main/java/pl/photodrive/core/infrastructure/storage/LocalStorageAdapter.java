package pl.photodrive.core.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.photodrive.core.domain.port.StoragePort;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
    public void store(String albumId, String fileName, InputStream content, long size) {

    }

    @Override
    public void delete(String albumId, String fileName) {

    }

    @Override
    public void rename(String albumId, String oldName, String newName) {

    }
}
