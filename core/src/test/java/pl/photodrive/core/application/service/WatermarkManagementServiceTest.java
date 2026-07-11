package pl.photodrive.core.application.service;

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

@ExtendWith(MockitoExtension.class)
class WatermarkManagementServiceTest {

    @Mock private WatermarkStorePort watermarkStore;
    @Mock private FileRepository fileRepository;
    @Mock private FileStoragePort fileStoragePort;

    @InjectMocks private WatermarkManagementService service;

    @Test
    void shouldStorePngAndClearCacheOnUpload() throws IOException {
        byte[] png = validPng();

        service.uploadWatermark(png);

        verify(watermarkStore).put(png);
        verify(fileStoragePort).clearWatermarkCache();
    }

    @Test
    void shouldRejectNonPngUpload() throws IOException {
        // Poprawny obraz, ale JPEG — logo musi być PNG (przezroczystość)
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);

        assertThrows(FileException.class, () -> service.uploadWatermark(baos.toByteArray()));
        verify(watermarkStore, never()).put(any());
    }

    @Test
    void shouldRejectUndecodablePng() {
        // Magic PNG, ale dalej śmieci — ImageIO nie zdekoduje
        byte[] fake = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4, 5};

        assertThrows(FileException.class, () -> service.uploadWatermark(fake));
        verify(watermarkStore, never()).put(any());
    }

    @Test
    void shouldRejectTooLargeUpload() {
        byte[] huge = new byte[3 * 1024 * 1024];
        huge[0] = (byte) 0x89;
        huge[1] = 'P';
        huge[2] = 'N';
        huge[3] = 'G';

        assertThrows(FileException.class, () -> service.uploadWatermark(huge));
        verify(watermarkStore, never()).put(any());
    }

    @Test
    void shouldBlockDeleteWhenWatermarkInUse() {
        when(fileRepository.countWithWatermark()).thenReturn(5L);

        assertThrows(AlbumException.class, () -> service.deleteWatermark());
        verify(watermarkStore, never()).delete();
        verify(fileStoragePort, never()).clearWatermarkCache();
    }

    @Test
    void shouldDeleteAndClearCacheWhenNotInUse() {
        when(fileRepository.countWithWatermark()).thenReturn(0L);

        service.deleteWatermark();

        verify(watermarkStore).delete();
        verify(fileStoragePort).clearWatermarkCache();
    }

    private byte[] validPng() throws IOException {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }
}
