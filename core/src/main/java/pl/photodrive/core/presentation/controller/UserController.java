package pl.photodrive.core.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.photodrive.core.application.command.AddUserCommand;
import pl.photodrive.core.application.service.UserManagementService;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.presentation.dto.CreateUserRequest;
import pl.photodrive.core.presentation.dto.UserDto;
import pl.photodrive.core.presentation.mapper.ApiMappers;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserManagementService userService;

    @GetMapping
    public List<UserDto> getAll(){
        return userService.getAllUsers().stream().map(ApiMappers::toDto).toList();
    }

    @PostMapping
    public ResponseEntity<UserDto> add(@Valid @RequestBody CreateUserRequest request) {
        Email email = new Email(request.email());
        var created = userService.addUser(new AddUserCommand(request.name(),email,request.password()));
        return ResponseEntity.created(URI.create("/api/users/" + created.getId())).body(ApiMappers.toDto(created));
    }
}
