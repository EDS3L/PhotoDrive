package pl.photodrive.core.presentation.web.cookie;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TokenCookieWriter {

    private final boolean secure;

    public TokenCookieWriter(@Value("${app.cookie.secure:true}") boolean secure) {
        this.secure = secure;
    }

    public ResponseCookie accessTokenCookie(String jwt, Duration ttl) {
        return ResponseCookie.from("pd_at", jwt)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/")
                .maxAge(ttl)
                .build();
    }

    public ResponseCookie deleteAccessTokenCookie() {
        return ResponseCookie.from("pd_at", "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
    }
}
