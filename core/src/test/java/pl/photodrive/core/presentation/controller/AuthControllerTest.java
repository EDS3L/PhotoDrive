package pl.photodrive.core.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.photodrive.core.application.command.auth.RemindPasswordCommand;
import pl.photodrive.core.application.dto.AccessToken;
import pl.photodrive.core.application.exception.LoginFailedException;
import pl.photodrive.core.application.service.AuthManagerService;
import pl.photodrive.core.application.service.TokenManagementService;
import pl.photodrive.core.domain.exception.PasswordTokenException;
import pl.photodrive.core.infrastructure.jwt.JwtAuthenticationFilter;
import pl.photodrive.core.presentation.dto.user.LoginRequest;
import pl.photodrive.core.presentation.dto.user.RemindPasswordRequest;
import pl.photodrive.core.presentation.web.cookie.TokenCookieWriter;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @MockitoBean
    private AuthManagerService authManagerService;

        @MockitoBean
    private TokenManagementService tokenManagementService;

        @MockitoBean
    private TokenCookieWriter tokenCookieWriter;

    // -----------------------------------------------------------------------
    // POST /api/auth/login
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Successful login sets the HttpOnly session cookie")
    void shouldReturn200WithCookieOnSuccessfulLogin() throws Exception {
        // Given
        AccessToken token = new AccessToken("jwt.token.here", Duration.ofMinutes(15));
        given(authManagerService.login(any())).willReturn(token);
        given(tokenCookieWriter.accessTokenCookie(any(), any()))
                .willReturn(ResponseCookie.from("pd_at", "jwt.token.here").build());

        String body = objectMapper.writeValueAsString(new LoginRequest("user@example.com", "Pass1!"));

        // When / Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    @DisplayName("Wrong credentials return 401")
    void shouldReturn401WhenCredentialsInvalid() throws Exception {
        // Given
        given(authManagerService.login(any())).willThrow(new LoginFailedException("Invalid credentials!"));

        String body = objectMapper.writeValueAsString(new LoginRequest("user@example.com", "wrong"));

        // When / Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Blank login payload is rejected by validation")
    void shouldReturn400WhenLoginRequestBodyIsBlank() throws Exception {
        // When / Then - empty email and password should fail @Valid
        String body = objectMapper.writeValueAsString(Map.of("email", "", "password", ""));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // POST /api/auth/logout
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Logout clears the session cookie")
    void shouldReturn200OnLogout() throws Exception {
        // Given
        given(tokenCookieWriter.deleteAccessTokenCookie())
                .willReturn(ResponseCookie.from("pd_at", "").maxAge(0).build());

        // When / Then
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk());
    }

    // -----------------------------------------------------------------------
    // POST /api/auth/remindPassword
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Password reset forwards e-mail, authorization code and new password each into its own field, so none of the three can be silently swapped")
    void shouldForwardResetFieldsIntoTheirOwnCommandFields() throws Exception {
        // Given - three values distinct enough that any swap shows up in the assertion
        UUID code = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(
                new RemindPasswordRequest("klient@example.com", code, "NoweHaslo123!"));

        // When
        mockMvc.perform(post("/api/auth/remindPassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Then - a swap here is a SILENT bug: the request still answers 200, only the password
        // does not become what the user typed
        ArgumentCaptor<RemindPasswordCommand> command = ArgumentCaptor.forClass(RemindPasswordCommand.class);
        then(authManagerService).should().remindPassword(command.capture());
        assertThat(command.getValue().email()).isEqualTo("klient@example.com");
        assertThat(command.getValue().token()).isEqualTo(code);
        assertThat(command.getValue().newPassword()).isEqualTo("NoweHaslo123!");
    }

    @Test
    @DisplayName("Every reset failure is reported as the same 400, so a wrong code cannot be told apart from an unknown account")
    void shouldReturn400WhenResetFails() throws Exception {
        // Given - the service refuses (unknown e-mail, missing, expired or wrong code alike)
        willThrow(new PasswordTokenException("Nieprawidłowy lub wygasły kod autoryzacji."))
                .given(authManagerService).remindPassword(any());

        String body = objectMapper.writeValueAsString(
                new RemindPasswordRequest("klient@example.com", UUID.randomUUID(), "NoweHaslo123!"));

        // When / Then - 400 for all four cases is what closes account enumeration (B.14)
        mockMvc.perform(post("/api/auth/remindPassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A reset without an authorization code never reaches the service")
    void shouldReturn400WhenResetCodeMissing() throws Exception {
        // Given - token omitted entirely
        String body = objectMapper.writeValueAsString(
                Map.of("email", "klient@example.com", "newPassword", "NoweHaslo123!"));

        // When / Then
        mockMvc.perform(post("/api/auth/remindPassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        then(authManagerService).shouldHaveNoInteractions();
    }

    // -----------------------------------------------------------------------
    // POST /api/auth/create/passwordToken/{email}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Requesting an authorization code returns 200")
    void shouldReturn200WhenCreatingPasswordToken() throws Exception {
        // When / Then
        mockMvc.perform(post("/api/auth/create/passwordToken/user@example.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unknown email also returns 200, so accounts cannot be enumerated")
    void shouldReturn200WhenUserNotFoundForPasswordTokenToAvoidEnumeration() throws Exception {
        // When / Then
        mockMvc.perform(post("/api/auth/create/passwordToken/unknown@example.com"))
                .andExpect(status().isOk());

        then(tokenManagementService).should().createToken("unknown@example.com");
    }
}
