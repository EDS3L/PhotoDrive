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

    /**
     * Wariant zdjęcia dla strony publicznej: przeskalowany tak, by <b>dłuższy bok</b> nie
     * przekraczał {@code maxDimension} (nigdy nie powiększa). Cache:
     * {@code {storage}/.cache/public/{cacheKey}} — klucz zawiera fileId i rozmiar, więc wpisy
     * nigdy nie kolidują i można je skasować w każdej chwili.
     *
     * <p>Sens: gość NIGDY nie dostaje oryginału (A9). Skalowanie „po dłuższym boku", a nie po
     * szerokości, zamyka obejście pionowym kadrem (portret 2560 szerokości = 3840 wysokości).
     *
     * <p>Wariant publiczny jest <b>zawsze czysty</b> — portfolio nie bywa watermarkowane
     * (reguła w {@code Album}), więc ta ścieżka w ogóle nie zna znaku wodnego.
     */
    Resource getOrCreatePublicPhoto(String albumPath, String fileName, String cacheKey, int maxDimension);

    void swapFile(String albumPath, String targetPath, String fileName);
}
