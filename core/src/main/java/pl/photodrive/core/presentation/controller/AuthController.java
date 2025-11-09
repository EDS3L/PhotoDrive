package pl.photodrive.core.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.photodrive.core.application.command.auth.LoginCommand;
import pl.photodrive.core.application.service.AuthManagerService;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.presentation.dto.user.LoginRequest;
import pl.photodrive.core.presentation.web.cookie.TokenCookieWriter;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthManagerService authManagerService;
    private final TokenCookieWriter tokenCookieWriter;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
        LoginCommand cmd = new LoginCommand(new Email(request.email()), request.password());
        var accessToken = authManagerService.login(cmd);
        var cookie = tokenCookieWriter.accessTokenCookie(accessToken.value(),accessToken.ttl());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        var cookie = tokenCookieWriter.deleteAccessTokenCookie();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }
}
