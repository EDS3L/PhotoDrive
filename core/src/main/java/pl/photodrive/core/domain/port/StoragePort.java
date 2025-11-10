package pl.photodrive.core.domain.port;

import java.io.InputStream;

public interface StoragePort {
    void createFolderForPhotograph(String email);
    void createClientAlbumDir(String name,String photographFolder);
    void createAdminAlbumDir(String name);
    void store(String photographEmail, String fileName);
    void storeToAlbum(String photographEmail,String albumName, String fileName, InputStream fileData);
    void delete(String albumId, String fileName);
    void rename(String albumId, String oldName,String newName);
}
