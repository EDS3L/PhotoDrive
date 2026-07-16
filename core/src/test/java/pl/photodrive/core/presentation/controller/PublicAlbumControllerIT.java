package pl.photodrive.core.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import pl.photodrive.core.application.port.repository.AlbumRepository;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.support.IntegrationTest;
import pl.photodrive.core.support.TestFixtures;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Portfolio jest dostępne BEZ logowania, więc publiczny endpoint zdjęć jest jedynym miejscem,
 * gdzie przypadkowy internauta może sięgnąć po pracę fotografa. Ten test pilnuje reguły z A9:
 * <b>gość dostaje wariant, nigdy oryginał</b> — niezależnie od tego, o co poprosi.
 *
 * <p>Sprawdzamy to na prawdziwym pliku (upload → dysk → HTTP → dekod odpowiedzi), bo tylko wtedy
 * widać RZECZYWISTE wymiary tego, co wyszło z serwera.
 */
class PublicAlbumControllerIT extends IntegrationTest {

    /** Zdjęcie „z aparatu": dużo większe niż limit, żeby cap miał co ścinać. */
    private static final int ORIGINAL_WIDTH = 4000;
    private static final int ORIGINAL_HEIGHT = 3000;
    private static final int PUBLIC_CAP = 2560;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AlbumRepository albumRepository;

    private User admin;

    @BeforeEach
    void seedAdmin() {
        admin = fixtures.admin("admin@photodrive.dev");
    }

    @Test
    @DisplayName("Guest gets a downscaled photo, so the full-resolution original never leaves the server")
    void shouldServeCappedPhotoToAnonymousGuest() throws Exception {
        // Given - a published album with a 4000px photo
        UUID albumId = publishedAlbumWithPhoto("portfolio-sluby", "foto.jpg");

        // When - a guest asks for the photo without any size (and WITHOUT a session cookie)
        BufferedImage served = readPublicPhoto(albumId, "foto.jpg", null);

        // Then - the long edge is capped; the guest cannot get the print-resolution file
        assertThat(Math.max(served.getWidth(), served.getHeight())).isEqualTo(PUBLIC_CAP);
        assertThat(served.getWidth()).isLessThan(ORIGINAL_WIDTH);
    }

    @Test
    @DisplayName("Asking for a huge size does not lift the cap, so the limit cannot be talked out of")
    void shouldIgnoreOversizedRequestFromGuest() throws Exception {
        // Given
        UUID albumId = publishedAlbumWithPhoto("portfolio-plener", "foto.jpg");

        // When - the guest tries to talk the server into the original
        BufferedImage served = readPublicPhoto(albumId, "foto.jpg", 9999);

        // Then
        assertThat(Math.max(served.getWidth(), served.getHeight())).isEqualTo(PUBLIC_CAP);
    }

    @Test
    @DisplayName("A size below the cap is honoured, which is what keeps the portfolio grid light")
    void shouldHonourSmallSizeRequestedByTheGrid() throws Exception {
        // Given
        UUID albumId = publishedAlbumWithPhoto("portfolio-portret", "foto.jpg");

        // When
        BufferedImage served = readPublicPhoto(albumId, "foto.jpg", 800);

        // Then
        assertThat(Math.max(served.getWidth(), served.getHeight())).isEqualTo(800);
    }

    @Test
    @DisplayName("Serving downscaled variants leaves the original on disk untouched, so the photographer keeps his file")
    void shouldLeaveTheOriginalOnDiskUntouched() throws Exception {
        // Given
        UUID albumId = publishedAlbumWithPhoto("portfolio-reportaz", "foto.jpg");
        Path original = storageRoot().resolve("portfolio-reportaz").resolve("foto.jpg");
        byte[] beforeServing = Files.readAllBytes(original);

        // When
        readPublicPhoto(albumId, "foto.jpg", null);

        // Then - byte for byte the same file, and still at full resolution
        assertThat(Files.readAllBytes(original)).isEqualTo(beforeServing);
        assertThat(ImageIO.read(original.toFile()).getWidth()).isEqualTo(ORIGINAL_WIDTH);
    }

    @Test
    @DisplayName("A photo the admin never revealed stays out of the portfolio, whatever size is asked for")
    void shouldRefuseHiddenPhotoOnThePublicEndpoint() throws Exception {
        // Given - admin uploads are visible by default (B.5), so to test the hidden path the admin
        // must explicitly hide the photo again before it can be treated as withdrawn from portfolio
        UUID albumId = createAdminAlbum("portfolio-ukryte");
        uploadPhoto(albumId, "ukryte.jpg");
        setVisible(albumId, photoId(albumId), false);
        setPublic(albumId);

        // When / Then
        mockMvc.perform(get("/api/public/album/{albumId}/photo/{fileName}", albumId, "ukryte.jpg"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Portfolio tabs come back in the admin-chosen order with human labels, so the site never shows raw technical names")
    void shouldOrderPublicListingByDisplayOrderAndCarryLabels() throws Exception {
        // Given - alphabetical order (aa < zz) is the OPPOSITE of the chosen display order,
        // so only a real displayOrder sort can produce the expected sequence
        UUID second = createAdminAlbum("aa-portrety-it");
        UUID first = createAdminAlbum("zz-sluby-it");
        setPublic(first);
        setPublic(second);
        setDisplay(first, "Śluby", 1);
        setDisplay(second, "Portrety", 2);

        // When - an anonymous visitor asks for the portfolio listing
        MvcResult result = mockMvc.perform(get("/api/public/album/all"))
                .andExpect(status().isOk())
                .andReturn();

        // Then - relative order follows displayOrder and the Unicode label rides along
        var albums = objectMapper.readTree(result.getResponse().getContentAsString());
        int posFirst = -1;
        int posSecond = -1;
        String labelFirst = null;
        for (int i = 0; i < albums.size(); i++) {
            String name = albums.get(i).get("name").asText();
            if ("zz-sluby-it".equals(name)) {
                posFirst = i;
                labelFirst = albums.get(i).get("displayName").asText();
            }
            if ("aa-portrety-it".equals(name)) {
                posSecond = i;
            }
        }
        assertThat(posFirst).as("zz-sluby-it must be in the public listing").isNotNegative();
        assertThat(posSecond).as("aa-portrety-it must be in the public listing").isNotNegative();
        assertThat(posFirst).as("displayOrder must beat alphabetical order").isLessThan(posSecond);
        assertThat(labelFirst).isEqualTo("Śluby");
    }

    // --- helpers -----------------------------------------------------------

    private UUID publishedAlbumWithPhoto(String albumName, String fileName) throws Exception {
        // No explicit setVisible: uploads to an admin (portfolio) album are visible at once (B.5).
        UUID albumId = createAdminAlbum(albumName);
        uploadPhoto(albumId, fileName);
        setPublic(albumId);
        return albumId;
    }

    private UUID createAdminAlbum(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/album/admin/create")
                        .cookie(fixtures.authCookie(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}""".formatted(name)))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("albumId").asText());
    }

    private void uploadPhoto(UUID albumId, String fileName) throws Exception {
        MockMultipartFile photo = new MockMultipartFile("files", fileName, "image/jpeg",
                TestFixtures.jpeg(ORIGINAL_WIDTH, ORIGINAL_HEIGHT));
        mockMvc.perform(multipart("/api/album/upload/{albumId}/files", albumId)
                        .file(photo)
                        .cookie(fixtures.authCookie(admin)))
                .andExpect(status().isAccepted());
    }

    private void setVisible(UUID albumId, UUID fileId, boolean visible) throws Exception {
        mockMvc.perform(patch("/api/album/{albumId}/files/setVisible", albumId)
                        .param("visible", String.valueOf(visible))
                        .cookie(fixtures.authCookie(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idList":["%s"]}""".formatted(fileId)))
                .andExpect(status().isOk());
    }

    private void setPublic(UUID albumId) throws Exception {
        mockMvc.perform(patch("/api/album/{albumId}/setPublic", albumId)
                        .param("isPublic", "true")
                        .cookie(fixtures.authCookie(admin)))
                .andExpect(status().isOk());
    }

    private void setDisplay(UUID albumId, String displayName, int displayOrder) throws Exception {
        mockMvc.perform(patch("/api/album/{albumId}/display", albumId)
                        .cookie(fixtures.authCookie(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"%s","displayOrder":%d}""".formatted(displayName, displayOrder)))
                .andExpect(status().isOk());
    }

    /** Żądanie BEZ cookie — dokładnie tak, jak robi to przeglądarka przypadkowego gościa. */
    private BufferedImage readPublicPhoto(UUID albumId, String fileName, Integer width) throws Exception {
        var request = get("/api/public/album/{albumId}/photo/{fileName}", albumId, fileName);
        if (width != null) {
            request = request.param("width", String.valueOf(width));
        }
        byte[] body = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        return ImageIO.read(new ByteArrayInputStream(body));
    }

    /** Odczyt agregatu w transakcji — mapowanie encja→domena jest leniwe (patrz {@code IntegrationTest}). */
    private UUID photoId(UUID albumId) {
        return inTransaction(() -> albumRepository.findByAlbumId(new AlbumId(albumId))
                .orElseThrow(() -> new AssertionError("Album " + albumId + " is not in the database"))
                .getPhotos().values().stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Album " + albumId + " has no photos"))
                .getFileId().value());
    }
}
