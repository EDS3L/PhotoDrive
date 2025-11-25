package pl.photodrive.core.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.photodrive.core.application.command.user.*;
import pl.photodrive.core.application.service.UserManagementService;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.Password;
import pl.photodrive.core.domain.vo.UserId;
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

    @GetMapping
    public List<UserDto> getAll(){
        return userService.getAllUsers().stream().map(ApiMappers::toDto).toList();
    }

    @PostMapping("/add")
    public ResponseEntity<UserDto> add(@Valid @RequestBody CreateUserRequest request) {
        Email email = new Email(request.email());
        Password password = new Password(request.password());
        var created = userService.addUser(new AddUserCommand(request.name(),email, password.value(), request.role()));
        return ResponseEntity.created(URI.create("/api/users/add" + created.getId().value())).body(ApiMappers.toDto(created));
    }

    @PatchMapping("/{id}/addRole")
    public ResponseEntity<UserDto> addRole(@Valid @RequestBody RoleRequest request, @PathVariable UUID id) {
        UserId userId = new UserId(id);
        var updated = userService.addRole(new RoleCommand(userId,request.role()));
        return ResponseEntity.ok().body(ApiMappers.toDto(updated));
    }

    @PatchMapping("/{id}/removeRole")
    public ResponseEntity<UserDto> removeRole(@Valid @RequestBody RoleRequest request, @PathVariable UUID id) {
        UserId userId = new UserId(id);
        var updated = userService.removeRole(new RoleCommand(userId, request.role()));
        return ResponseEntity.ok().body(ApiMappers.toDto(updated));
    }

    @PatchMapping("/{id}/changPassword")
    public ResponseEntity<UserDto> changePassword(@Valid @RequestBody PasswordRequest request, @PathVariable UUID id) {
        UserId userId = new UserId(id);
        userService.changePassword(new ChangePasswordCommand(userId,request.currentPassword(), request.newPassword()));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/changeEmail")
    public ResponseEntity<UserDto> changeEmail(@Valid @RequestBody EmailRequest request, @PathVariable UUID id) {
        UserId userId = new UserId(id);
        var updated = userService.changeEmail(new ChangeEmailCommand(userId,request.newEmail()));
        return ResponseEntity.ok().body(ApiMappers.toDto(updated));
    }

    @PatchMapping("/{id}/activateUser")
    public ResponseEntity<UserDto> activateUser(@Valid @RequestBody boolean active, @PathVariable UUID id) {
        UserId userId = new UserId(id);
        var user = userService.activateUser(new ActivateUserCommand(userId,active));
        return ResponseEntity.ok().body(ApiMappers.toDto(user));
    }

    @PatchMapping("/{id}/deactivateUser")
    public ResponseEntity<UserDto> deactivateUser(@Valid @RequestBody boolean active, @PathVariable UUID id) {
        UserId userId = new UserId(id);
        var user = userService.deactiveUser(new ActivateUserCommand(userId,active));
        return ResponseEntity.ok().body(ApiMappers.toDto(user));
    }
}
