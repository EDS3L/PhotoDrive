package pl.photodrive.core.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.photodrive.core.application.command.auth.LoginCommand;
import pl.photodrive.core.application.service.AuthManagerService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AuthController {

    private final AuthManagerService authManagerService;

    @PostMapping("/login")
    public ResponseEntity<String> login(LoginCommand cmd) {
        var accessToken = authManagerService.login(cmd);
        return ResponseEntity.ok().body(accessToken);
    }
}
