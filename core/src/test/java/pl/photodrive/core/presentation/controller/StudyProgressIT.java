package pl.photodrive.core.presentation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.photodrive.core.domain.vo.StudySyncCode;
import pl.photodrive.core.support.IntegrationTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Synchronizacja postępu nauki na żywym torze: anonimowe żądanie przechodzi przez filtry
 * (Origin, rate limit, JWT) do prawdziwego MySQL i wraca drugim urządzeniem.
 */
class StudyProgressIT extends IntegrationTest {

    private static final String CODE = "k7Qp2xWmZ-a_9LrT0bNc4e";
    private static final String PATH = "/api/public/study/progress/" + CODE;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Progress saved anonymously from one device is read back by another, and a later save replaces it")
    void shouldCarryProgressBetweenDevicesWithoutLogin() throws Exception {
        // Given - one device saves its progress (no cookie, same-site Origin like a browser sends)
        mockMvc.perform(put(PATH)
                        .header("Origin", "http://localhost:3000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"learned":[9,1]}"""))
                .andExpect(status().isOk());

        // When / Then - the other device reads the same, normalised progress with a timestamp
        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learned.length()").value(2))
                .andExpect(jsonPath("$.learned[0]").value(1))
                .andExpect(jsonPath("$.learned[1]").value(9))
                .andExpect(jsonPath("$.updatedAt").isNumber());

        // When - the other device unmarks everything
        mockMvc.perform(put(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"learned":[]}"""))
                .andExpect(status().isOk());

        // Then - the first device now reads the new state
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath("$.learned").isEmpty())
                .andExpect(jsonPath("$.updatedAt").isNumber());
    }

    @Test
    @DisplayName("The database holds only the hash of the code, so reading the table does not hand out anyone's sync key")
    void shouldStoreOnlyTheHashOfTheCode() throws Exception {
        // Given
        mockMvc.perform(put(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"learned":[5]}"""))
                .andExpect(status().isOk());

        // When
        List<String> keys = jdbcTemplate.queryForList("SELECT codeHash FROM study_progress", String.class);

        // Then
        assertThat(keys).containsExactly(new StudySyncCode(CODE).hashed());
    }

    @Test
    @DisplayName("A save from a foreign site is refused by the Origin check, like every other write to the API")
    void shouldRefuseCrossSiteSave() throws Exception {
        // When / Then
        mockMvc.perform(put(PATH)
                        .header("Origin", "https://evil.example")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"learned":[1]}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A code that was never used reads as empty progress with no timestamp")
    void shouldReadUnknownCodeAsEmpty() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/public/study/progress/AAAAAAAAAAAAAAAAAAAAAA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learned").isEmpty())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());
    }
}
