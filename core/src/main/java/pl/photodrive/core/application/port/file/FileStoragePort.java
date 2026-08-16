package pl.photodrive.core.application.port.file;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface FileStoragePort {
    void createPhotographerFolder(String photographerEmail);

    void renamePhotographerFolder(String oldEmail, String newEmail);

    void createClientAlbum(String albumName, String photographerEmail);

    void createAdminAlbum(String albumName);

    void saveFile(String path, String fileName, InputStream fileData) throws IOException;

    void deleteFile(String path, String fileName);

    void renameFile(String path, String oldName, String newName);

    byte[] createZipArchive(String albumPath, List<String> fileNames, Map<String, String> watermarkCacheKeys, byte[] watermarkPng);

    void deleteFolder(String albumPath);

    Resource getOrCreateWatermarkedPhoto(String albumPath, String fileName, String cacheKey, boolean thumbnail, byte[] watermarkPng);

    void clearWatermarkCache();

    Resource getOrCreatePublicPhoto(String albumPath, String fileName, String cacheKey, int maxDimension);

    void swapFile(String albumPath, String targetPath, String fileName);
}
