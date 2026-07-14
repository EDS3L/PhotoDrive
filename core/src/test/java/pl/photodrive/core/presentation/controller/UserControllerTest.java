package pl.photodrive.core.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import pl.photodrive.core.application.exception.ApplicationSecurityException;
import pl.photodrive.core.application.service.UserManagementService;
import pl.photodrive.core.domain.exception.UserException;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.UserId;
import pl.photodrive.core.infrastructure.jwt.JwtAuthenticationFilter;
import pl.photodrive.core.presentation.dto.user.UserDto;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @MockitoBean
    private UserManagementService userService;

    // -----------------------------------------------------------------------
    // GET /api/user/all
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Admin receives the user list")
    void shouldReturn200WithUsersListWhenGetAll() throws Exception {
        // Given
        UserDto dto = aUserDto();
        given(userService.getAllUsers()).willReturn(List.of());

        // When / Then
        mockMvc.perform(get("/api/user/all"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Non-admin is refused the user list")
    void shouldReturn403WhenGetAllForbidden() throws Exception {
        // Given
        given(userService.getAllUsers()).willThrow(new ApplicationSecurityException("Forbidden!"));

        // When / Then
        mockMvc.perform(get("/api/user/all"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("SECURITY_EXCEPTION"));
    }

    // -----------------------------------------------------------------------
    // POST /api/user/add
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Creating a user returns 201")
    void shouldReturn201WhenUserCreated() throws Exception {
        // Given
        var user = aUserDomainStub();
        given(userService.addUser(any())).willReturn(user);

        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Jan Kowalski",
                "email", "jan@example.com",
                "role", "ADMIN"
        ));

        // When / Then
        mockMvc.perform(post("/api/user/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Incomplete user payload is rejected by validation")
    void shouldReturn400WhenAddUserWithMissingFields() throws Exception {
        // When / Then - missing required fields triggers @Valid → MethodArgumentNotValidException → 400
        String body = objectMapper.writeValueAsString(Map.of("email", ""));

        mockMvc.perform(post("/api/user/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_EXCEPTION"));
    }

    // -----------------------------------------------------------------------
    // PATCH /api/user/{id}/changeEmail
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Email change returns 200")
    void shouldReturn200WhenEmailChangedSuccessfully() throws Exception {
        // Given
        var user = aUserDomainStub();
        given(userService.changeEmail(any())).willReturn(user);

        UUID id = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(Map.of("newEmail", "new@example.com"));

        // When / Then
        mockMvc.perform(patch("/api/user/{id}/changeEmail", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Non-admin cannot change another user's email")
    void shouldReturn403WhenNonAdminAttemptsToChangeOtherUsersEmail() throws Exception {
        // Given
        given(userService.changeEmail(any()))
                .willThrow(new ApplicationSecurityException("Access denied!"));

        UUID id = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(Map.of("newEmail", "attacker@example.com"));

        // When / Then
        mockMvc.perform(patch("/api/user/{id}/changeEmail", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("SECURITY_EXCEPTION"));
    }

    // -----------------------------------------------------------------------
    // PATCH /api/user/{id}/changePassword
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Password change returns 204")
    void shouldReturn204WhenPasswordChangedSuccessfully() throws Exception {
        // No mock needed - void method, default does nothing

        UUID id = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(
                Map.of("currentPassword", "OldPass1!", "newPassword", "NewPass2@"));

        // When / Then
        mockMvc.perform(patch("/api/user/{id}/changePassword", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Password change for an unknown user returns 400")
    void shouldReturn400WhenUserNotFoundOnPasswordChange() throws Exception {
        // Given
        willThrow(new UserException("User not found!")).given(userService).changePassword(any());

        UUID id = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(
                Map.of("currentPassword", "OldPass1!", "newPassword", "NewPass2@"));

        // When / Then
        mockMvc.perform(patch("/api/user/{id}/changePassword", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("USER_EXCEPTION"));
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private UserDto aUserDto() {
        return new UserDto(
                UserId.newId().value().toString(),
                "Test User",
                "test@example.com",
                Set.of(Role.ADMIN),
                true,
                false,
                List.of()
        );
    }

    private pl.photodrive.core.domain.model.User aUserDomainStub() {
        return new pl.photodrive.core.domain.model.User(
                UserId.newId(),
                "Jan Kowalski",
                new Email("jan@example.com"),
                new pl.photodrive.core.domain.vo.HashedPassword("$2a$10$hashedPassword"),
                Set.of(Role.ADMIN),
                false,   // changePasswordOnNextLogin
                true,    // isActive
                List.of()
        );
    }
}
