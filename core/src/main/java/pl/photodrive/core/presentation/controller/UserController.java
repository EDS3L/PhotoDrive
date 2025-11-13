package pl.photodrive.core.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.photodrive.core.application.command.user.AddUserCommand;
import pl.photodrive.core.application.command.user.ChangeEmailCommand;
import pl.photodrive.core.application.command.user.ChangePasswordCommand;
import pl.photodrive.core.application.command.user.RoleCommand;
import pl.photodrive.core.application.service.UserManagementService;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.Password;
import pl.photodrive.core.domain.vo.UserId;
import pl.photodrive.core.presentation.dto.user.*;
import pl.photodrive.core.presentation.mapper.ApiMappers;

import java.net.URI;
import java.util.List;

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
    public ResponseEntity<UserDto> addRole(@Valid @RequestBody RoleRequest request, @PathVariable UserId id) {
        var updated = userService.addRole(new RoleCommand(id,request.role()));
        return ResponseEntity.ok().body(ApiMappers.toDto(updated));
    }


    @PatchMapping("/{id}/removeRole")
    public ResponseEntity<UserDto> removeRole(@Valid @RequestBody RoleRequest request, @PathVariable UserId id) {
        var updated = userService.removeRole(new RoleCommand(id, request.role()));
        return ResponseEntity.ok().body(ApiMappers.toDto(updated));
    }

    @PatchMapping("/{id}/changPassword")
    public ResponseEntity<UserDto> changePassword(@Valid @RequestBody PasswordRequest request, @PathVariable UserId id) {
        userService.changePassword(new ChangePasswordCommand(id,request.currentPassword(), request.newPassword()));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/changeEmail")
    public ResponseEntity<UserDto> changeEmail(@Valid @RequestBody EmailRequest request, @PathVariable UserId id) {
        var updated = userService.changeEmail(new ChangeEmailCommand(id,request.newEmail()));
        return ResponseEntity.ok().body(ApiMappers.toDto(updated));
    }



}
