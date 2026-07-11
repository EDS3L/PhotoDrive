package pl.photodrive.core.application.port.file;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface FileStoragePort {
    void createPhotographerFolder(String photographerEmail);

    void createClientAlbum(String albumName, String photographerEmail);

    void createAdminAlbum(String albumName);

    void saveFile(String path, String fileName, InputStream fileData) throws IOException;

    void deleteFile(String path, String fileName);

    void renameFile(String path, String oldName, String newName);

    // watermarkCacheKeys: nazwa pliku -> klucz cache (fileId-wersjaLoga) dla plików, które
    // w ZIP-ie mają iść w wersji watermarkowanej (pobieranie klienta); pusta mapa = same oryginały.
    byte[] createZipArchive(String albumPath, List<String> fileNames, Map<String, String> watermarkCacheKeys, byte[] watermarkPng);

    void deleteFolder(String albumPath);

    /**
     * Zwraca watermarkowaną wersję zdjęcia: z cache ({storage}/.cache/watermark/{cacheKey}-{wariant}),
     * a przy braku — komponuje kafelki w locie z oryginału/miniatury i zapisuje do cache.
     * Cache jest jednorazowego użytku (klucz po fileId+wersji loga) — można go skasować w każdej chwili.
     * Oryginał na dysku pozostaje nietknięty.
     */
    Resource getOrCreateWatermarkedPhoto(String albumPath, String fileName, String cacheKey, boolean thumbnail, byte[] watermarkPng);

    /** Czyści cały cache watermarkowanych wersji (wołane przy podmianie/usunięciu loga). */
    void clearWatermarkCache();

    void swapFile(String albumPath, String targetPath, String fileName);
}
