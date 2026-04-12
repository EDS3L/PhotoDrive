package pl.photodrive.core.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
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
import pl.photodrive.core.application.dto.AccessToken;
import pl.photodrive.core.application.exception.LoginFailedException;
import pl.photodrive.core.application.service.AuthManagerService;
import pl.photodrive.core.application.service.TokenManagementService;
import pl.photodrive.core.domain.exception.UserException;
import pl.photodrive.core.infrastructure.jwt.JwtAuthenticationFilter;
import pl.photodrive.core.presentation.dto.user.LoginRequest;
import pl.photodrive.core.presentation.web.cookie.TokenCookieWriter;

import java.time.Duration;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
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
    void shouldReturn400WhenLoginRequestBodyIsBlank() throws Exception {
        // When / Then — empty email and password should fail @Valid
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
    void shouldReturn200OnLogout() throws Exception {
        // Given
        given(tokenCookieWriter.deleteAccessTokenCookie())
                .willReturn(ResponseCookie.from("pd_at", "").maxAge(0).build());

        // When / Then
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk());
    }

    // -----------------------------------------------------------------------
    // POST /api/auth/create/passwordToken/{email}
    // -----------------------------------------------------------------------

    @Test
    void shouldReturn200WhenCreatingPasswordToken() throws Exception {
        // When / Then
        mockMvc.perform(post("/api/auth/create/passwordToken/user@example.com"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WhenUserNotFoundForPasswordToken() throws Exception {
        // Given
        doThrow(new UserException("User not found!"))
                .when(tokenManagementService).createToken("unknown@example.com");

        // When / Then
        mockMvc.perform(post("/api/auth/create/passwordToken/unknown@example.com"))
                .andExpect(status().isBadRequest());
    }
}
