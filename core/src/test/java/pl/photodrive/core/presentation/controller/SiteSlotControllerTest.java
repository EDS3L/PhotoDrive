package pl.photodrive.core.presentation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.photodrive.core.application.port.site.SiteSlot;
import pl.photodrive.core.application.port.site.SiteSlotVersion;
import pl.photodrive.core.application.service.SiteSlotManagementService;
import pl.photodrive.core.domain.exception.FileException;
import pl.photodrive.core.infrastructure.jwt.JwtAuthenticationFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Warstwa webowa slotów strony wizytówki (panel admina). Reguły przygotowania obrazu
 * (skalowanie do 2560, re-enkodowanie) mają własne testy w {@code SiteSlotManagementServiceTest};
 * tutaj: kształt odpowiedzi i mapowanie wyjątków na HTTP. Kto ma prawo tu wejść, pilnuje
 * {@code SecurityAuthorizationIT} — dlatego security jest świadomie wyłączone.
 */
@WebMvcTest(controllers = SiteSlotController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
class SiteSlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SiteSlotManagementService slotService;

    @Test
    @DisplayName("The panel listing carries every slot from the enum, including empty ones, so the admin sees what can be configured")
    void shouldListEverySlotIncludingUnconfigured() throws Exception {
        // Given - only one slot has a photo
        given(slotService.getConfiguredSlots()).willReturn(List.of(
                new SiteSlotVersion(SiteSlot.HOME_HERO, Instant.ofEpochMilli(1234))));

        // When / Then - all enum slots are present (enum order); only HOME_HERO reports configured
        mockMvc.perform(get("/api/site/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(SiteSlot.values().length))
                .andExpect(jsonPath("$[0].slot").value("HOME_HERO"))
                .andExpect(jsonPath("$[0].configured").value(true))
                .andExpect(jsonPath("$[0].updatedAt").exists())
                .andExpect(jsonPath("$[3].slot").value("ABOUT_BIO"))
                .andExpect(jsonPath("$[3].configured").value(false));
    }

    @Test
    @DisplayName("An uploaded photo reaches the service byte for byte, so validation and scaling judge the real file")
    void shouldPassUploadedBytesToServiceAndReturn204() throws Exception {
        // Given
        byte[] photo = {(byte) 0xFF, (byte) 0xD8, 1, 2, 3};
        MockMultipartFile file = new MockMultipartFile("file", "hero.jpg", MediaType.IMAGE_JPEG_VALUE, photo);

        // When / Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/site/slots/HOME_HERO").file(file))
                .andExpect(status().isNoContent());

        then(slotService).should().upload(SiteSlot.HOME_HERO, photo);
    }

    @Test
    @DisplayName("An unknown slot key is the client's mistake (400), not a server error (500)")
    void shouldReturn400ForUnknownSlotKey() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "x.jpg", MediaType.IMAGE_JPEG_VALUE,
                new byte[]{1});

        // When / Then - upload and clear alike
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/site/slots/HOME_HERRO").file(file))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/site/slots/HOME_HERRO"))
                .andExpect(status().isBadRequest());

        then(slotService).should(never()).upload(any(), any());
        then(slotService).should(never()).clear(any());
    }

    @Test
    @DisplayName("A photo the service refuses is reported as a broken rule (400), so the admin can fix the file and retry")
    void shouldReturn400WhenServiceRejectsThePhoto() throws Exception {
        // Given - e.g. an undecodable file; the rule itself lives in the service
        willThrow(new FileException("Unsupported image format"))
                .given(slotService).upload(any(), any());
        MockMultipartFile file = new MockMultipartFile("file", "x.webp", "image/webp", new byte[]{1, 2});

        // When / Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/site/slots/HOME_HERO").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("An upload that cannot be read fails as a 400 instead of leaking an IOException as a 500")
    void shouldReturn400WhenUploadedFileCannotBeRead() throws Exception {
        // Given - a part that blows up while being read (truncated upload, broken stream)
        MockMultipartFile unreadable = new MockMultipartFile("file", "x.jpg", MediaType.IMAGE_JPEG_VALUE,
                new byte[]{1}) {
            @Override
            public byte[] getBytes() throws IOException {
                throw new IOException("stream died");
            }
        };

        // When / Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/site/slots/HOME_HERO").file(unreadable))
                .andExpect(status().isBadRequest());

        then(slotService).should(never()).upload(any(), any());
    }

    @Test
    @DisplayName("Upload without the file part is rejected as a bad request, not as a server error")
    void shouldReturn400WhenFilePartIsMissing() throws Exception {
        // When / Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/site/slots/HOME_HERO"))
                .andExpect(status().isBadRequest());

        then(slotService).should(never()).upload(any(), any());
    }

    @Test
    @DisplayName("Clearing a slot answers 204 and targets exactly the requested slot")
    void shouldClearSlotAndReturn204() throws Exception {
        // When / Then
        mockMvc.perform(delete("/api/site/slots/ABOUT_BIO"))
                .andExpect(status().isNoContent());

        then(slotService).should().clear(SiteSlot.ABOUT_BIO);
    }
}
