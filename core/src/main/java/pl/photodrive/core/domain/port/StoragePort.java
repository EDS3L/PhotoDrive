package pl.photodrive.core.domain.port;

import org.springframework.core.io.Resource;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.List;

public interface StoragePort {
    void createFolderForPhotograph(String email);
    void createClientAlbumDir(String name,String photographFolder);
    void createAdminAlbumDir(String name);
    void storeByAdmin(String photographEmail, String fileName, InputStream fileData);
    void storeToAlbum(String photographEmail,String albumName, String fileName, InputStream fileData);
    StreamingResponseBody downloadSelectedFilesAsZip(String albumName, List<String> fileNames, String photographEmail);
    void delete(String albumId, String fileName);
    void rename(String albumId, String oldName,String newName);
}
