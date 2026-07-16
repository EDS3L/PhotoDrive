package pl.photodrive.core.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.photodrive.core.application.port.file.FileStoragePort;
import pl.photodrive.core.application.port.file.WatermarkStorePort;
import pl.photodrive.core.application.port.repository.FileRepository;
import pl.photodrive.core.domain.exception.AlbumException;
import pl.photodrive.core.domain.exception.FileException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class WatermarkManagementServiceTest {

    @Mock private WatermarkStorePort watermarkStore;
    @Mock private FileRepository fileRepository;
    @Mock private FileStoragePort fileStoragePort;

    @InjectMocks private WatermarkManagementService service;

    @Test
    @DisplayName("Uploading a logo stores it and wipes the watermark cache, so old renderings disappear")
    void shouldStorePngAndClearCacheOnUpload() throws IOException {
        // Given
        byte[] png = validPng();

        // When
        service.uploadWatermark(png);

        // Then
        then(watermarkStore).should().put(png);
        then(fileStoragePort).should().clearWatermarkCache();
    }

    @Test
    @DisplayName("Only a PNG is accepted as the platform logo")
    void shouldRejectNonPngUpload() throws IOException {
        // Given
        // A valid image, but JPEG - the logo must be PNG for transparency
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);

        // When
        assertThrows(FileException.class, () -> service.uploadWatermark(baos.toByteArray()));

        // Then
        then(watermarkStore).should(never()).put(any());
    }

    @Test
    @DisplayName("A file that claims to be a PNG but cannot be decoded is rejected")
    void shouldRejectUndecodablePng() {
        // Given
        // PNG magic bytes followed by garbage - ImageIO cannot decode it
        byte[] fake = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4, 5};

        // When
        assertThrows(FileException.class, () -> service.uploadWatermark(fake));

        // Then
        then(watermarkStore).should(never()).put(any());
    }

    @Test
    @DisplayName("An empty upload is rejected before anything reaches the store, so a 0-byte pick cannot wipe the logo")
    void shouldRejectEmptyUpload() {
        // Given - what the browser sends when a 0-byte file is picked
        byte[] empty = new byte[0];

        // When
        assertThrows(FileException.class, () -> service.uploadWatermark(empty));

        // Then
        then(watermarkStore).should(never()).put(any());
    }

    @Test
    @DisplayName("A file too short to even hold the PNG header is rejected instead of being read past its end")
    void shouldRejectFileShorterThanPngHeader() {
        // Given - 2 bytes: the magic-number check must not index past the array
        byte[] stub = {(byte) 0x89, 'P'};

        // When
        assertThrows(FileException.class, () -> service.uploadWatermark(stub));

        // Then
        then(watermarkStore).should(never()).put(any());
    }

    @Test
    @DisplayName("Logo larger than the size limit is rejected")
    void shouldRejectTooLargeUpload() {
        // Given
        byte[] huge = new byte[3 * 1024 * 1024];
        huge[0] = (byte) 0x89;
        huge[1] = 'P';
        huge[2] = 'N';
        huge[3] = 'G';

        // When
        assertThrows(FileException.class, () -> service.uploadWatermark(huge));

        // Then
        then(watermarkStore).should(never()).put(any());
    }

    @Test
    @DisplayName("Logo cannot be deleted while photos still carry the watermark flag")
    void shouldBlockDeleteWhenWatermarkInUse() {
        // Given
        given(fileRepository.countWithWatermark()).willReturn(5L);

        // When
        assertThrows(AlbumException.class, () -> service.deleteWatermark());

        // Then
        then(watermarkStore).should(never()).delete();
        then(fileStoragePort).should(never()).clearWatermarkCache();
    }

    @Test
    @DisplayName("Unused logo can be deleted and the cache is wiped with it")
    void shouldDeleteAndClearCacheWhenNotInUse() {
        // Given
        given(fileRepository.countWithWatermark()).willReturn(0L);

        // When
        service.deleteWatermark();

        // Then
        then(watermarkStore).should().delete();
        then(fileStoragePort).should().clearWatermarkCache();
    }

    private byte[] validPng() throws IOException {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }
}
