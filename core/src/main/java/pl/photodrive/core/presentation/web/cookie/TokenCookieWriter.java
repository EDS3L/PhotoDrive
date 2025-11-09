package pl.photodrive.core.presentation.web.cookie;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TokenCookieWriter {

    public ResponseCookie accessTokenCookie(String jwt, Duration ttl) {
        return ResponseCookie.from("pd_at", jwt)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(ttl)
                .build();
    }

    public ResponseCookie deleteAccessTokenCookie() {
        return ResponseCookie.from("pd_at","")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .build();
    }
}
