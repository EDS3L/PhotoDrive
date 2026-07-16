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
import pl.photodrive.core.application.port.file.PlatformWatermark;
import pl.photodrive.core.application.service.WatermarkManagementService;
import pl.photodrive.core.domain.exception.AlbumException;
import pl.photodrive.core.domain.exception.FileException;
import pl.photodrive.core.infrastructure.jwt.JwtAuthenticationFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Warstwa webowa jedynego globalnego znaku wodnego platformy (5.6). Reguły biznesowe (walidacja
 * PNG, blokada usunięcia loga będącego w użyciu) mają własne testy w
 * {@code WatermarkManagementServiceTest} — tutaj sprawdzamy to, za co odpowiada kontroler:
 * kształt odpowiedzi i mapowanie wyjątków na HTTP.
 *
 * <p>Kto ma prawo wejść na te endpointy, pilnuje {@code SecurityAuthorizationIT} (macierz
 * autoryzacji na żywym torze) — dlatego security jest tu świadomie wyłączone.
 */
@WebMvcTest(controllers = WatermarkController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
class WatermarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WatermarkManagementService watermarkService;

    // -----------------------------------------------------------------------
    // GET /api/watermark/status
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Status carries the logo version, because the panel hangs it on the preview URL to defeat the browser cache")
    void shouldReturnConfiguredStatusWithVersionWhenLogoExists() throws Exception {
        // Given
        given(watermarkService.getWatermark()).willReturn(Optional.of(
                new PlatformWatermark(new byte[]{1, 2, 3}, Instant.ofEpochMilli(1234))));

        // When / Then
        mockMvc.perform(get("/api/watermark/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("Status without a logo answers 200 with configured=false, so the UI can hide the watermark action instead of erroring")
    void shouldReturnNotConfiguredStatusWhenNoLogo() throws Exception {
        // Given
        given(watermarkService.getWatermark()).willReturn(Optional.empty());

        // When / Then - a missing logo is a normal state, not a failure
        mockMvc.perform(get("/api/watermark/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(false))
                .andExpect(jsonPath("$.updatedAt").doesNotExist());
    }

    // -----------------------------------------------------------------------
    // GET /api/watermark
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Logo is served as raw PNG bytes, so the admin preview shows the real file")
    void shouldReturnLogoImageWhenConfigured() throws Exception {
        // Given
        byte[] logo = {(byte) 0x89, 'P', 'N', 'G', 7, 7};
        given(watermarkService.getWatermark()).willReturn(Optional.of(
                new PlatformWatermark(logo, Instant.ofEpochMilli(1234))));

        // When / Then
        mockMvc.perform(get("/api/watermark"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(logo));
    }

    @Test
    @DisplayName("Asking for a logo that was never uploaded gives 404, not an empty 200")
    void shouldReturn404WhenNoLogoToServe() throws Exception {
        // Given
        given(watermarkService.getWatermark()).willReturn(Optional.empty());

        // When / Then
        mockMvc.perform(get("/api/watermark"))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // PUT /api/watermark
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Uploaded logo reaches the service byte for byte, so validation judges the real file")
    void shouldPassUploadedBytesToServiceAndReturn204() throws Exception {
        // Given
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 1, 2};
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", MediaType.IMAGE_PNG_VALUE, png);

        // When / Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/watermark").file(file))
                .andExpect(status().isNoContent());

        then(watermarkService).should().uploadWatermark(png);
    }

    @Test
    @DisplayName("A logo the service refuses is reported as a broken rule (400), so the admin can fix the file and retry")
    void shouldReturn400WhenServiceRejectsTheLogo() throws Exception {
        // Given - e.g. not a PNG; the rule itself lives in the service
        willThrow(new FileException("Watermark must be a PNG image"))
                .given(watermarkService).uploadWatermark(any());
        MockMultipartFile file = new MockMultipartFile("file", "logo.jpg", MediaType.IMAGE_JPEG_VALUE,
                new byte[]{1, 2, 3});

        // When / Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/watermark").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("An upload that cannot be read fails as a 400 instead of leaking an IOException as a 500")
    void shouldReturn400WhenUploadedFileCannotBeRead() throws Exception {
        // Given - a part that blows up while being read (truncated upload, broken stream)
        MockMultipartFile unreadable = new MockMultipartFile("file", "logo.png", MediaType.IMAGE_PNG_VALUE,
                new byte[]{1}) {
            @Override
            public byte[] getBytes() throws IOException {
                throw new IOException("stream died");
            }
        };

        // When / Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/watermark").file(unreadable))
                .andExpect(status().isBadRequest());

        then(watermarkService).should(never()).uploadWatermark(any());
    }

    @Test
    @DisplayName("Upload without the file part is rejected as a bad request, not as a server error")
    void shouldReturn400WhenFilePartIsMissing() throws Exception {
        // When / Then
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/watermark"))
                .andExpect(status().isBadRequest());

        then(watermarkService).should(never()).uploadWatermark(any());
    }

    // -----------------------------------------------------------------------
    // DELETE /api/watermark
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Deleting an unused logo answers 204")
    void shouldDeleteWatermarkAndReturn204() throws Exception {
        // When / Then
        mockMvc.perform(delete("/api/watermark"))
                .andExpect(status().isNoContent());

        then(watermarkService).should().deleteWatermark();
    }

    @Test
    @DisplayName("Refusing to delete a logo that photos still use is a broken rule (400), so protected photos are never silently stripped")
    void shouldReturn400WhenDeletingLogoStillInUse() throws Exception {
        // Given - the guard itself lives in the service; here we pin how it reaches the client
        willThrow(new AlbumException("Cannot delete watermark: it is applied to 5 photo(s)."))
                .given(watermarkService).deleteWatermark();

        // When / Then
        mockMvc.perform(delete("/api/watermark"))
                .andExpect(status().isBadRequest());
    }
}
