package pl.photodrive.core.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.photodrive.core.application.port.site.SiteSlot;
import pl.photodrive.core.application.port.site.SiteSlotStorePort;
import pl.photodrive.core.domain.exception.FileException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SiteSlotManagementServiceTest {

    @Mock private SiteSlotStorePort slotStore;

    @InjectMocks private SiteSlotManagementService service;

    @Test
    @DisplayName("A slot photo is stored already capped at 2560 px on the longer edge, so the original never reaches the public site")
    void shouldDownscaleOversizedUploadBeforeStoring() throws IOException {
        // Given - a photo well above the public cap (landscape 3600x2400)
        byte[] original = jpeg(3600, 2400);

        // When
        service.upload(SiteSlot.HOME_HERO, original);

        // Then - what hits the store is the capped variant, not the original
        BufferedImage stored = storedImage();
        assertThat(Math.max(stored.getWidth(), stored.getHeight())).isEqualTo(2560);
        // aspect ratio survives the downscale (3600:2400 = 3:2 -> 2560:1707)
        assertThat(stored.getHeight()).isEqualTo(1707);
    }

    @Test
    @DisplayName("The cap applies to the longer edge, so a portrait photo cannot sneak past it with a small width")
    void shouldCapPortraitPhotoByItsHeight() throws IOException {
        // Given - portrait: width is far below the cap, height is far above
        byte[] portrait = jpeg(2000, 4000);

        // When
        service.upload(SiteSlot.ABOUT_BIO, portrait);

        // Then
        BufferedImage stored = storedImage();
        assertThat(stored.getHeight()).isEqualTo(2560);
        assertThat(stored.getWidth()).isEqualTo(1280);
    }

    @Test
    @DisplayName("A photo already within the cap keeps its dimensions instead of being blurred by an upscale")
    void shouldNotUpscaleSmallPhoto() throws IOException {
        // Given
        byte[] small = jpeg(800, 600);

        // When
        service.upload(SiteSlot.HOME_INTRO, small);

        // Then
        BufferedImage stored = storedImage();
        assertThat(stored.getWidth()).isEqualTo(800);
        assertThat(stored.getHeight()).isEqualTo(600);
    }

    @Test
    @DisplayName("Every upload is re-encoded as JPEG, so camera metadata (EXIF/GPS) never lands on the public site")
    void shouldReencodePngUploadAsJpeg() throws IOException {
        // Given - a PNG upload (the format itself carries the point: output must not stay PNG)
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);

        // When
        service.upload(SiteSlot.HOME_CTA, baos.toByteArray());

        // Then - stored bytes start with the JPEG magic number, not the PNG one
        byte[] stored = storedBytes();
        assertThat(stored[0]).isEqualTo((byte) 0xFF);
        assertThat(stored[1]).isEqualTo((byte) 0xD8);
    }

    @Test
    @DisplayName("An empty upload is rejected before anything reaches the store, so a 0-byte pick cannot wipe the slot")
    void shouldRejectEmptyUpload() {
        // When
        assertThrows(FileException.class, () -> service.upload(SiteSlot.HOME_HERO, new byte[0]));

        // Then
        then(slotStore).should(never()).put(any(), any());
    }

    @Test
    @DisplayName("A file ImageIO cannot decode (webp/heic/corrupted) is rejected with a clear rule, not an NPE")
    void shouldRejectUndecodableUpload() {
        // Given - bytes that are not any image ImageIO understands
        byte[] garbage = {1, 2, 3, 4, 5, 6, 7, 8};

        // When
        assertThrows(FileException.class, () -> service.upload(SiteSlot.HOME_HERO, garbage));

        // Then
        then(slotStore).should(never()).put(any(), any());
    }

    @Test
    @DisplayName("An upload above the size limit is rejected before decoding, so a huge file cannot exhaust the heap")
    void shouldRejectOversizedUpload() {
        // Given - just over 30 MB
        byte[] huge = new byte[30 * 1024 * 1024 + 1];

        // When
        assertThrows(FileException.class, () -> service.upload(SiteSlot.HOME_HERO, huge));

        // Then
        then(slotStore).should(never()).put(any(), any());
    }

    @Test
    @DisplayName("Clearing a slot removes exactly that slot from the store")
    void shouldClearSlot() {
        // When
        service.clear(SiteSlot.ABOUT_EQUIPMENT);

        // Then
        then(slotStore).should().delete(SiteSlot.ABOUT_EQUIPMENT);
    }

    // --- helpers -----------------------------------------------------------

    private byte[] storedBytes() {
        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        then(slotStore).should().put(any(SiteSlot.class), captor.capture());
        return captor.getValue();
    }

    private BufferedImage storedImage() throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(storedBytes()));
        assertThat(image).as("stored bytes must decode as an image").isNotNull();
        return image;
    }

    private static byte[] jpeg(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }
}
