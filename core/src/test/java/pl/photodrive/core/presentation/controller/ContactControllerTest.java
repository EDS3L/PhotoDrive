package pl.photodrive.core.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.photodrive.core.application.command.contact.ContactCommand;
import pl.photodrive.core.application.service.ContactService;
import pl.photodrive.core.infrastructure.jwt.JwtAuthenticationFilter;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ContactController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ContactService contactService;

    private Map<String, Object> validPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "Jan Kowalski");
        payload.put("email", "jan@example.com");
        payload.put("phone", "+48 111 222 333");
        payload.put("sessionType", "Fotografia ślubna");
        payload.put("message", "Dzień dobry, chciałbym zapytać o wolny termin.");
        return payload;
    }

    @Test
    @DisplayName("A valid contact form is accepted and forwarded to the service as a command carrying the same fields")
    void shouldAcceptValidContactFormAndForwardCommand() throws Exception {
        // When / Then
        mockMvc.perform(post("/api/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isOk());

        then(contactService).should().handle(new ContactCommand(
                "Jan Kowalski", "jan@example.com", "+48 111 222 333",
                "Fotografia ślubna", "Dzień dobry, chciałbym zapytać o wolny termin."));
    }

    @Test
    @DisplayName("A blank message is rejected by validation, so an empty enquiry never reaches the mailbox")
    void shouldReject400WhenMessageIsBlank() throws Exception {
        // Given
        Map<String, Object> payload = validPayload();
        payload.put("message", "");

        // When / Then
        mockMvc.perform(post("/api/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        then(contactService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("A malformed e-mail is rejected, so the notification has a real Reply-To to answer")
    void shouldReject400WhenEmailIsMalformed() throws Exception {
        // Given
        Map<String, Object> payload = validPayload();
        payload.put("email", "not-an-email");

        // When / Then
        mockMvc.perform(post("/api/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        then(contactService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("A too-short message is rejected, so one-character spam does not trigger a mail")
    void shouldReject400WhenMessageTooShort() throws Exception {
        // Given
        Map<String, Object> payload = validPayload();
        payload.put("message", "hej");

        // When / Then
        mockMvc.perform(post("/api/public/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        then(contactService).shouldHaveNoInteractions();
    }
}
