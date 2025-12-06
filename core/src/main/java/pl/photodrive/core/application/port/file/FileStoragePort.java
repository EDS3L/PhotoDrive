package pl.photodrive.core.application.port.file;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface FileStoragePort {
    void createPhotographerFolder(String photographerEmail);

    void createClientAlbum(String albumName, String photographerEmail);

    void createAdminAlbum(String albumName);

    void saveFile(String path, String fileName, InputStream fileData) throws IOException;

    InputStream getFile(String path, String fileName);

    void deleteFile(String path, String fileName);

    void renameFile(String path, String oldName, String newName);

    byte[] createZipArchive(String albumPath, List<String> fileNames);

    void deleteFolder(String albumPath);

    void addWatermark(String path);
}
