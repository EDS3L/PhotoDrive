package pl.photodrive.core.presentation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.photodrive.core.application.port.site.SiteSlot;
import pl.photodrive.core.application.port.site.SiteSlotImage;
import pl.photodrive.core.application.port.site.SiteSlotVersion;
import pl.photodrive.core.application.service.SiteSlotManagementService;
import pl.photodrive.core.infrastructure.jwt.JwtAuthenticationFilter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Publiczny odczyt slotów strony wizytówki. Kluczowa jest tu semantyka cache: listing ma być
 * krótki (podmiana widoczna w ~30 s), a obraz agresywny (URL niesie wersję, więc jest niezmienny).
 */
@WebMvcTest(controllers = PublicSiteController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
class PublicSiteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SiteSlotManagementService slotService;

    @Test
    @DisplayName("The public listing carries only configured slots, so a section with no photo falls back to its placeholder")
    void shouldListOnlyConfiguredSlots() throws Exception {
        // Given - one configured slot out of five
        given(slotService.getConfiguredSlots()).willReturn(List.of(
                new SiteSlotVersion(SiteSlot.HOME_HERO, Instant.ofEpochMilli(7777))));

        // When / Then - HOME_HERO with its version; nothing else
        mockMvc.perform(get("/api/public/site/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slot").value("HOME_HERO"))
                .andExpect(jsonPath("$[0].version").value(7777));
    }

    @Test
    @DisplayName("The listing is cached briefly, so swapping a photo shows up on the public site within ~30 seconds")
    void shouldServeListingWithShortCache() throws Exception {
        // Given
        given(slotService.getConfiguredSlots()).willReturn(List.of());

        // When / Then
        mockMvc.perform(get("/api/public/site/slots"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("max-age=30")));
    }

    @Test
    @DisplayName("The photo itself is served as immutable, because its URL carries the version and changes on every swap")
    void shouldServePhotoWithImmutableCache() throws Exception {
        // Given
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, 5, 6};
        given(slotService.getImage(SiteSlot.HOME_HERO)).willReturn(Optional.of(
                new SiteSlotImage(jpeg, Instant.ofEpochMilli(7777))));

        // When / Then
        mockMvc.perform(get("/api/public/site/photo/HOME_HERO"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(jpeg))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("immutable")));
    }

    @Test
    @DisplayName("Asking for a photo of an empty slot gives 404, not an empty 200 the browser would cache for a year")
    void shouldReturn404ForEmptySlot() throws Exception {
        // Given
        given(slotService.getImage(SiteSlot.ABOUT_BIO)).willReturn(Optional.empty());

        // When / Then
        mockMvc.perform(get("/api/public/site/photo/ABOUT_BIO"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("An unknown slot key on the public path is a bad request, not a server error")
    void shouldReturn400ForUnknownSlotKey() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/public/site/photo/NOT_A_SLOT"))
                .andExpect(status().isBadRequest());
    }
}
