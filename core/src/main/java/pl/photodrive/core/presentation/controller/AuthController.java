package pl.photodrive.core.presentation.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.photodrive.core.application.command.auth.LoginCommand;
import pl.photodrive.core.application.command.auth.RemindPasswordCommand;
import pl.photodrive.core.application.service.AuthManagerService;
import pl.photodrive.core.application.service.TokenManagementService;
import pl.photodrive.core.presentation.dto.user.LoginRequest;
import pl.photodrive.core.presentation.dto.user.RemindPasswordRequest;
import pl.photodrive.core.presentation.web.cookie.TokenCookieWriter;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthManagerService authManagerService;
    private final TokenCookieWriter tokenCookieWriter;
    private final TokenManagementService tokenManagementService;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
        LoginCommand cmd = new LoginCommand(request.email(), request.password());
        var accessToken = authManagerService.login(cmd);
        var cookie = tokenCookieWriter.accessTokenCookie(accessToken.value(), accessToken.ttl());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        var cookie = tokenCookieWriter.deleteAccessTokenCookie();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @PostMapping("/remindPassword")
    public ResponseEntity<Void> responseEntity(RemindPasswordRequest request) {
        RemindPasswordCommand remindPasswordCommand = new RemindPasswordCommand(request.email(),
                request.token(),
                request.newPassword());
        authManagerService.remindPassword(remindPasswordCommand);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/create/passwordToken/{email}")
    public ResponseEntity<Void> createPasswordToken(@NotNull @PathVariable String email) {
        tokenManagementService.createToken(email);
        return ResponseEntity.ok().build();
    }
}
