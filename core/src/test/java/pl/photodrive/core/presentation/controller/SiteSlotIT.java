package pl.photodrive.core.presentation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.support.IntegrationTest;
import pl.photodrive.core.support.TestFixtures;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cały tor slotów strony wizytówki na żywej bazie: admin wgrywa zdjęcie przez HTTP,
 * gość (bez cookie!) czyta listing i pobiera obraz. Test dekoduje realnie zwrócone
 * bajty — pilnuje capu 2560 na tym, co NAPRAWDĘ wychodzi z serwera, nie na mocku.
 */
class SiteSlotIT extends IntegrationTest {

    @Test
    @DisplayName("A photo uploaded by the admin reaches an anonymous visitor already capped at 2560 px, never as the original")
    void shouldServeUploadedSlotPhotoToAnonymousVisitorCapped() throws Exception {
        // Given - an admin and a photo above the public cap (3200x2000)
        User admin = fixtures.admin("slot-admin@photodrive.dev");
        MockMultipartFile photo = new MockMultipartFile("file", "hero.jpg", MediaType.IMAGE_JPEG_VALUE,
                TestFixtures.jpeg(3200, 2000));

        // When - the admin fills the HOME_HERO slot
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/site/slots/HOME_HERO")
                        .file(photo)
                        .cookie(fixtures.authCookie(admin)))
                .andExpect(status().isNoContent());

        // Then - the public listing (no cookie) shows the slot with a version
        mockMvc.perform(get("/api/public/site/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slot == 'HOME_HERO')]").exists());

        // Then - the served image (no cookie) decodes and its longer edge is exactly the cap
        MvcResult imageResult = mockMvc.perform(get("/api/public/site/photo/HOME_HERO"))
                .andExpect(status().isOk())
                .andReturn();
        BufferedImage served = ImageIO.read(
                new ByteArrayInputStream(imageResult.getResponse().getContentAsByteArray()));
        assertThat(served).as("served bytes must decode as an image").isNotNull();
        assertThat(Math.max(served.getWidth(), served.getHeight())).isEqualTo(2560);
    }

    @Test
    @DisplayName("A cleared slot disappears from the public site: the listing drops it and the photo answers 404")
    void shouldRemoveClearedSlotFromThePublicSite() throws Exception {
        // Given - a configured slot
        User admin = fixtures.admin("slot-admin2@photodrive.dev");
        MockMultipartFile photo = new MockMultipartFile("file", "bio.jpg", MediaType.IMAGE_JPEG_VALUE,
                TestFixtures.jpeg(1000, 800));
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/site/slots/ABOUT_BIO")
                        .file(photo)
                        .cookie(fixtures.authCookie(admin)))
                .andExpect(status().isNoContent());

        // When - the admin clears it
        mockMvc.perform(delete("/api/site/slots/ABOUT_BIO")
                        .cookie(fixtures.authCookie(admin)))
                .andExpect(status().isNoContent());

        // Then - gone from the listing, photo is a 404
        mockMvc.perform(get("/api/public/site/slots"))
                .andExpect(jsonPath("$[?(@.slot == 'ABOUT_BIO')]").doesNotExist());
        mockMvc.perform(get("/api/public/site/photo/ABOUT_BIO"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Replacing a slot photo bumps its version, so a browser holding the old immutable URL fetches the new image")
    void shouldBumpVersionWhenPhotoIsReplaced() throws Exception {
        // Given - a slot with a photo and its published version
        User admin = fixtures.admin("slot-admin3@photodrive.dev");
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/site/slots/HOME_CTA")
                        .file(new MockMultipartFile("file", "a.jpg", MediaType.IMAGE_JPEG_VALUE,
                                TestFixtures.jpeg(900, 600)))
                        .cookie(fixtures.authCookie(admin)))
                .andExpect(status().isNoContent());
        long firstVersion = slotVersion("HOME_CTA");

        // Wersja to updatedAt w milisekundach — bez tej przerwy dwa uploady mogłyby wpaść
        // w tę samą milisekundę i wersja by nie drgnęła (flake). Krótkie, deterministyczne
        // opóźnienie gwarantuje, że Instant.now() drugiego uploadu jest ściśle większy.
        Thread.sleep(5);

        // When - the photo is replaced
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/site/slots/HOME_CTA")
                        .file(new MockMultipartFile("file", "b.jpg", MediaType.IMAGE_JPEG_VALUE,
                                TestFixtures.jpeg(901, 601)))
                        .cookie(fixtures.authCookie(admin)))
                .andExpect(status().isNoContent());

        // Then - the version moved forward (the image URL embeds it, so the cache busts itself)
        assertThat(slotVersion("HOME_CTA")).isGreaterThan(firstVersion);
    }

    // --- helpers -----------------------------------------------------------

    private long slotVersion(String slot) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/public/site/slots"))
                .andExpect(status().isOk())
                .andReturn();
        var root = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString());
        for (var node : root) {
            if (slot.equals(node.get("slot").asText())) {
                return node.get("version").asLong();
            }
        }
        throw new AssertionError("Slot " + slot + " is not in the public listing");
    }
}
