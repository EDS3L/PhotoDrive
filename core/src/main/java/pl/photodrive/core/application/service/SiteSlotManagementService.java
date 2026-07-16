package pl.photodrive.core.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.photodrive.core.application.port.site.SiteSlot;
import pl.photodrive.core.application.port.site.SiteSlotImage;
import pl.photodrive.core.application.port.site.SiteSlotStorePort;
import pl.photodrive.core.application.port.site.SiteSlotVersion;
import pl.photodrive.core.domain.exception.FileException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Zdjęcia slotów strony wizytówki (hero, sekcje „o mnie" — {@link SiteSlot}). Wgrywa/podmienia
 * wyłącznie ADMIN; czyta strona publiczna bez logowania.
 *
 * <p>Serwer przy uploadzie od razu skaluje zdjęcie do {@link #SLOT_MAX_DIMENSION} po dłuższym
 * boku i zapisuje JPEG. Dzięki temu w bazie NIGDY nie leży oryginał (ten sam powód co cap A9a:
 * strona publiczna nie może być kanałem dystrybucji plików do druku), publiczny endpoint
 * serwuje bajty wprost bez przetwarzania, a re-enkodowanie usuwa przy okazji metadane
 * EXIF (w tym GPS) z pliku prosto z aparatu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteSlotManagementService {

    /** Ten sam limit co publiczne warianty albumów ({@code PUBLIC_MAX_DIMENSION}, A9a). */
    public static final int SLOT_MAX_DIMENSION = 2560;

    private static final long MAX_UPLOAD_SIZE_BYTES = 30L * 1024 * 1024;
    private static final float JPEG_QUALITY = 0.85f;

    private final SiteSlotStorePort slotStore;

    @Transactional(readOnly = true)
    public List<SiteSlotVersion> getConfiguredSlots() {
        return slotStore.findVersions();
    }

    @Transactional(readOnly = true)
    public Optional<SiteSlotImage> getImage(SiteSlot slot) {
        return slotStore.find(slot);
    }

    @Transactional
    public void upload(SiteSlot slot, byte[] image) {
        BufferedImage decoded = decodeOrThrow(image);
        byte[] prepared = encodeJpeg(downscale(decoded, SLOT_MAX_DIMENSION));
        slotStore.put(slot, prepared);
        log.info("Site slot {} updated ({} bytes uploaded, {} bytes stored)", slot, image.length, prepared.length);
    }

    @Transactional
    public void clear(SiteSlot slot) {
        slotStore.delete(slot);
        log.info("Site slot {} cleared", slot);
    }

    private BufferedImage decodeOrThrow(byte[] image) {
        if (image == null || image.length == 0) {
            throw new FileException("Slot image is empty");
        }
        if (image.length > MAX_UPLOAD_SIZE_BYTES) {
            throw new FileException("Slot image is too large (max 30 MB)");
        }
        try {
            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(image));
            if (decoded == null) {
                // ImageIO nie zna formatu (webp/heic/uszkodzony plik) — zwraca null, nie wyjątek.
                throw new FileException("Unsupported image format — upload a JPG or PNG photo");
            }
            return decoded;
        } catch (IOException e) {
            throw new FileException("Cannot decode slot image");
        }
    }

    private static BufferedImage downscale(BufferedImage image, int maxDimension) {
        int longEdge = Math.max(image.getWidth(), image.getHeight());
        if (longEdge <= maxDimension) {
            // Bez powiększania; redraw i tak jest potrzebny, żeby spłaszczyć ewentualny
            // kanał alfa (PNG) do RGB — JPEG nie zna przezroczystości.
            return redraw(image, image.getWidth(), image.getHeight());
        }

        double scale = (double) maxDimension / longEdge;
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
        return scaleTo(image, width, height);
    }

    /**
     * Skalowanie schodkowe, po połowie na krok — ta sama zasada co w {@code LocalStorageAdapter}:
     * jeden skok {@code drawImage} próbkuje tylko sąsiedztwo 2×2, więc gubi detale (banding).
     */
    private static BufferedImage scaleTo(BufferedImage image, int targetWidth, int targetHeight) {
        BufferedImage current = image;
        int width = image.getWidth();
        int height = image.getHeight();

        while (width / 2 >= targetWidth && height / 2 >= targetHeight) {
            width = Math.max(targetWidth, width / 2);
            height = Math.max(targetHeight, height / 2);
            current = redraw(current, width, height);
        }

        if (width != targetWidth || height != targetHeight) {
            current = redraw(current, targetWidth, targetHeight);
        }

        return current;
    }

    private static BufferedImage redraw(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = target.createGraphics();
        // Białe tło pod ewentualną przezroczystością PNG — default (czarny) wygląda jak błąd.
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(source, 0, 0, width, height, null);
        g2d.dispose();
        return target;
    }

    private static byte[] encodeJpeg(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (writers.hasNext()) {
                ImageWriter writer = writers.next();
                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(JPEG_QUALITY);
                }
                try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                    writer.setOutput(ios);
                    writer.write(null, new IIOImage(image, null, null), param);
                } finally {
                    writer.dispose();
                }
            } else {
                ImageIO.write(image, "jpg", baos);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new FileException("Cannot encode slot image");
        }
    }
}
