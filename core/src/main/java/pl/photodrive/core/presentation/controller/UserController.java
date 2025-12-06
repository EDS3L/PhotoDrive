package pl.photodrive.core.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.photodrive.core.application.command.user.*;
import pl.photodrive.core.application.service.UserManagementService;
import pl.photodrive.core.domain.vo.Password;
import pl.photodrive.core.presentation.dto.user.*;
import pl.photodrive.core.presentation.mapper.ApiMappers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserManagementService userService;

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


    @PostMapping("/add")
    public ResponseEntity<UserDto> add(@Valid @RequestBody CreateUserRequest request) {
        Password password = new Password(request.password());
        var created = userService.addUser(new AddUserCommand(request.name(),
                request.email(),
                password.value(),
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

    @PatchMapping("/{id}/changPassword")
    public ResponseEntity<UserDto> changePassword(@Valid @RequestBody PasswordRequest request, @PathVariable UUID id) {
        userService.changePassword(new ChangePasswordCommand(id, request.currentPassword(), request.newPassword()));
        return ResponseEntity.noContent().build();
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
        var user = userService.deactiveUser(new ActivateUserCommand(id, active));
        return ResponseEntity.ok().body(ApiMappers.toDto(user));
    }

    @PatchMapping("/{id}/removeUsers")
    public ResponseEntity<Void> removeUsers(@PathVariable UUID id, @RequestBody AssignUserRequest request) {
        userService.disconnectUsersFromPhotographer(new AssignUserCommand(request.userIdList(), id));
        return ResponseEntity.ok().build();
    }


    @PatchMapping("/{id}/assignUsers")
    public ResponseEntity<Void> assignUsers(@PathVariable UUID id, @RequestBody AssignUserRequest request) {
        userService.assignUsersToPhotograph(new AssignUserCommand(request.userIdList(), id));
        return ResponseEntity.ok().build();
    }
}
