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
import pl.photodrive.core.application.service.StudyProgressService;
import pl.photodrive.core.domain.exception.StudyProgressException;
import pl.photodrive.core.domain.model.StudyProgress;
import pl.photodrive.core.infrastructure.jwt.JwtAuthenticationFilter;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StudyProgressController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
class StudyProgressControllerTest {

    private static final String CODE = "k7Qp2xWmZ-a_9LrT0bNc4e";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudyProgressService service;

    @Test
    @DisplayName("Progress is returned with its timestamp and is never cached, so a device always sees the latest state")
    void shouldReturnProgressWithoutCaching() throws Exception {
        // Given
        given(service.get(CODE)).willReturn(new StudyProgress(List.of(1, 9), Instant.ofEpochMilli(1234)));

        // When / Then
        mockMvc.perform(get("/api/public/study/progress/" + CODE))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.learned[0]").value(1))
                .andExpect(jsonPath("$.learned[1]").value(9))
                .andExpect(jsonPath("$.updatedAt").value(1234));
    }

    @Test
    @DisplayName("Never-saved progress comes back with a null timestamp, which tells the client to upload its own")
    void shouldReturnNullTimestampForNeverSavedProgress() throws Exception {
        // Given
        given(service.get(CODE)).willReturn(StudyProgress.empty());

        // When / Then
        mockMvc.perform(get("/api/public/study/progress/" + CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learned").isEmpty())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());
    }

    @Test
    @DisplayName("Saving returns the stored progress, so the client can confirm what the server kept")
    void shouldSaveAndReturnProgress() throws Exception {
        // Given
        given(service.save(CODE, List.of(3, 1))).willReturn(new StudyProgress(List.of(1, 3), Instant.ofEpochMilli(99)));

        // When / Then
        mockMvc.perform(put("/api/public/study/progress/" + CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"learned":[3,1]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learned[0]").value(1))
                .andExpect(jsonPath("$.updatedAt").value(99));
    }

    @Test
    @DisplayName("A broken rule (bad code or question number) is a 400, not a server error")
    void shouldMapRuleViolationToBadRequest() throws Exception {
        // Given
        willThrow(new StudyProgressException("Invalid sync code")).given(service).get("short");

        // When / Then
        mockMvc.perform(get("/api/public/study/progress/short"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("STUDY_PROGRESS_EXCEPTION"));
    }

    @Test
    @DisplayName("A body that is not a list of numbers is a 400, not a server error")
    void shouldRejectMalformedBody() throws Exception {
        // When / Then
        mockMvc.perform(put("/api/public/study/progress/" + CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"learned":"all"}"""))
                .andExpect(status().isBadRequest());
    }
}
