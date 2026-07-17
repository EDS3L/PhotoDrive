package pl.photodrive.core.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pl.photodrive.core.application.command.user.*;
import pl.photodrive.core.application.port.token.TokenEncoder;
import pl.photodrive.core.application.service.UserManagementService;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.presentation.dto.user.*;
import pl.photodrive.core.presentation.mapper.ApiMappers;
import pl.photodrive.core.presentation.web.cookie.TokenCookieWriter;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserManagementService userService;
    private final TokenEncoder tokenEncoder;
    private final TokenCookieWriter tokenCookieWriter;
    private final Clock clock;

    @Value("${app.jwt.access-ttl-minutes:60}")
    private long accessTtlMinutes;

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> getCurrentUser() {
        var user = userService.getCurrentUser();
        var roles = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(new CurrentUserResponse(
                user.getId().value().toString(),
                user.getName(),
                user.getEmail().value(),
                roles,
                user.isChangePasswordOnNextLogin()
        ));
    }

    @GetMapping("/all")
    public List<UserDto> getAll() {
        return userService.getAllUsers().stream().map(ApiMappers::toDto).toList();
    }

    @GetMapping("/activeUsers")
    public List<UserDto> getAllActiveUsers() {
        return userService.getAllActiveUsers().stream().map(ApiMappers::toDto).toList();
    }

    @GetMapping("/getAssignedUsers")
    public List<UserDto> getAllAssignedUsers() {
        return userService.getPhotographUsers().stream().map(ApiMappers::toDto).toList();
    }

    @GetMapping("/{id}/assignedUsers")
    public List<UserDto> getPhotographerAssignedUsers(@PathVariable UUID id) {
        return userService.getPhotographerUsersForAdmin(id).stream().map(ApiMappers::toDto).toList();
    }


    @PostMapping("/add")
    public ResponseEntity<UserDto> add(@Valid @RequestBody CreateUserRequest request) {
        // Hasło startowe generowane serwerowo (nie wybierane przez twórcę konta);
        // użytkownik i tak musi je zmienić przy pierwszym logowaniu.
        var created = userService.addUser(new AddUserCommand(request.name(),
                request.email(),
                request.role()));
        return ResponseEntity.created(URI.create("/api/users/add" + created.getId().value())).body(ApiMappers.toDto(
                created));
    }

    @PatchMapping("/{id}/addRole")
    public ResponseEntity<UserDto> addRole(@Valid @RequestBody RoleRequest request, @PathVariable UUID id) {
        var updated = userService.addRole(new RoleCommand(id, request.role()));
        return ResponseEntity.ok().body(ApiMappers.toDto(updated));
    }

    @PatchMapping("/{id}/removeRole")
    public ResponseEntity<UserDto> removeRole(@Valid @RequestBody RoleRequest request, @PathVariable UUID id) {
        var updated = userService.removeRole(new RoleCommand(id, request.role()));
        return ResponseEntity.ok().body(ApiMappers.toDto(updated));
    }

    @PatchMapping("/{id}/changePassword")
    public ResponseEntity<UserDto> changePassword(@Valid @RequestBody PasswordRequest request, @PathVariable UUID id) {
        User updated = userService.changePassword(new ChangePasswordCommand(id, request.currentPassword(), request.newPassword()));

        // Zmiana WŁASNEGO hasła zdejmuje flagę wymuszonej zmiany, ale bieżący token wciąż ją
        // niesie (mustChangePassword=true) → filtr blokowałby usera (B.20) aż do slidingu.
        // Wystawiamy więc świeże, czyste cookie. Admin zmieniający CUDZE hasło re-issue nie dostaje
        // (nie ruszamy jego sesji cudzą tożsamością).
        if (isSelf(id)) {
            Duration ttl = Duration.ofMinutes(accessTtlMinutes);
            String jwt = tokenEncoder.createAccessToken(updated.getId(), updated.getRoles(), clock.instant(), ttl, false);
            return ResponseEntity.noContent()
                    .header(HttpHeaders.SET_COOKIE, tokenCookieWriter.accessTokenCookie(jwt, ttl).toString())
                    .build();
        }
        return ResponseEntity.noContent().build();
    }

    private boolean isSelf(UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && id.toString().equals(auth.getName());
    }

    @PatchMapping("/{id}/changeEmail")
    public ResponseEntity<UserDto> changeEmail(@Valid @RequestBody EmailRequest request, @PathVariable UUID id) {
        var updated = userService.changeEmail(new ChangeEmailCommand(id, request.newEmail()));
        return ResponseEntity.ok().body(ApiMappers.toDto(updated));
    }

    @PatchMapping("/{id}/activateUser")
    public ResponseEntity<UserDto> activateUser(@Valid @RequestBody boolean active, @PathVariable UUID id) {
        var user = userService.activateUser(new ActivateUserCommand(id, active));
        return ResponseEntity.ok().body(ApiMappers.toDto(user));
    }

    @PatchMapping("/{id}/deactivateUser")
    public ResponseEntity<UserDto> deactivateUser(@Valid @RequestBody boolean active, @PathVariable UUID id) {
        var user = userService.deactivateUser(new ActivateUserCommand(id, active));
        return ResponseEntity.ok().body(ApiMappers.toDto(user));
    }

    @PatchMapping("/{id}/removeUsers")
    public ResponseEntity<Void> removeUsers(@PathVariable UUID id, @Valid @RequestBody AssignUserRequest request) {
        userService.disconnectUsersFromPhotographer(new AssignUserCommand(request.userIdList(), id));
        return ResponseEntity.ok().build();
    }


    @PatchMapping("/{id}/assignUsers")
    public ResponseEntity<Void> assignUsers(@PathVariable UUID id, @Valid @RequestBody AssignUserRequest request) {
        userService.assignUsersToPhotograph(new AssignUserCommand(request.userIdList(), id));
        return ResponseEntity.ok().build();
    }
}
