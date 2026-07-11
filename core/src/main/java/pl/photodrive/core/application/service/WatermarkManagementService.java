package pl.photodrive.core.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.photodrive.core.application.port.file.FileStoragePort;
import pl.photodrive.core.application.port.file.PlatformWatermark;
import pl.photodrive.core.application.port.file.WatermarkStorePort;
import pl.photodrive.core.application.port.repository.FileRepository;
import pl.photodrive.core.domain.exception.AlbumException;
import pl.photodrive.core.domain.exception.FileException;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

/**
 * Zarządzanie JEDNYM globalnym znakiem wodnym platformy (wgrywa/podmienia/usuwa ADMIN).
 * Logo żyje w bazie (singleton) — podmiana/usunięcie czyści cache skomponowanych wersji.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatermarkManagementService {

    private static final long MAX_WATERMARK_SIZE_BYTES = 2 * 1024 * 1024;
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G'};

    private final WatermarkStorePort watermarkStore;
    private final FileRepository fileRepository;
    private final FileStoragePort fileStoragePort;

    @Transactional(readOnly = true)
    public Optional<PlatformWatermark> getWatermark() {
        return watermarkStore.get();
    }

    @Transactional
    public void uploadWatermark(byte[] image) {
        validateWatermarkImage(image);
        watermarkStore.put(image);
        // Stary cache = stare logo; klucz i tak zawiera wersję, ale sprzątamy od razu.
        fileStoragePort.clearWatermarkCache();
        log.info("Platform watermark uploaded ({} bytes)", image.length);
    }

    @Transactional
    public void deleteWatermark() {
        long inUse = fileRepository.countWithWatermark();
        if (inUse > 0) {
            // Bez tej blokady usunięcie loga po cichu odsłoniłoby chronione zdjęcia.
            throw new AlbumException("Cannot delete watermark: it is applied to " + inUse
                    + " photo(s). Remove the watermark from those photos first.");
        }
        watermarkStore.delete();
        fileStoragePort.clearWatermarkCache();
        log.info("Platform watermark deleted");
    }

    private void validateWatermarkImage(byte[] image) {
        if (image == null || image.length == 0) {
            throw new FileException("Watermark image is empty");
        }
        if (image.length > MAX_WATERMARK_SIZE_BYTES) {
            throw new FileException("Watermark image is too large (max 2 MB)");
        }
        if (!isPng(image)) {
            throw new FileException("Watermark must be a PNG image");
        }
        try {
            if (ImageIO.read(new ByteArrayInputStream(image)) == null) {
                throw new FileException("Cannot decode watermark image");
            }
        } catch (IOException e) {
            throw new FileException("Cannot decode watermark image");
        }
    }

    private static boolean isPng(byte[] data) {
        if (data.length < PNG_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < PNG_MAGIC.length; i++) {
            if (data[i] != PNG_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }
}
