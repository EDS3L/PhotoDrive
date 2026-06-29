package pl.photodrive.core.infrastructure.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import pl.photodrive.core.application.port.token.TokenDecoder;
import pl.photodrive.core.application.port.token.TokenEncoder;
import pl.photodrive.core.application.port.user.AuthenticatedUser;
import pl.photodrive.core.infrastructure.exception.ExpiredTokenException;
import pl.photodrive.core.infrastructure.exception.InvalidTokenException;
import pl.photodrive.core.presentation.web.cookie.TokenCookieWriter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String COOKIE_NAME = "pd_at";
    private static final String[] SKIP_PATHS = {
            "/api/auth/**",
            "/api/public/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/favicon.ico",
            "/error"
    };

    private final TokenDecoder tokenDecoder;
    private final TokenEncoder tokenEncoder;
    private final TokenCookieWriter tokenCookieWriter;
    private final Clock clock;

    @Value("${app.jwt.access-ttl-minutes:60}")
    private long accessTtlMinutes;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        for (String skipPath : SKIP_PATHS) {
            if (PATH_MATCHER.match(skipPath, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        TokenSource source = extractToken(request);

        if (source.value() == null || source.value().isBlank()) {
            sendUnauthorized(response, "Brak tokena uwierzytelniającego.");
            return;
        }

        try {
            var authentication = tokenDecoder.parse(source.value());
            var authorities = authentication.roles().stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                    .toList();

            var principal = new UsernamePasswordAuthenticationToken(
                    authentication.userId().value().toString(),
                    null,
                    authorities);

            SecurityContextHolder.getContext().setAuthentication(principal);

            // Sliding session: utrzymuje sesję przeglądarki dopóki użytkownik jest aktywny.
            // Odnawiamy tylko sesje cookie (tokeny Bearer należą do integracji API).
            if (source.fromCookie()) {
                renewIfExpiringSoon(authentication, response);
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredTokenException e) {
            sendUnauthorized(response, "Token wygasł.");
        } catch (InvalidTokenException e) {
            sendUnauthorized(response, "Nieprawidłowy token.");
        }
    }

    /**
     * Wystawia świeże cookie {@code pd_at} z nowym, pełnym tokenem, gdy bieżącemu
     * zostało mniej niż połowa życia. Efekt: każda aktywność (w tym żądanie uploadu)
     * przesuwa sesję 1h o kolejną godzinę do przodu.
     */
    private void renewIfExpiringSoon(AuthenticatedUser authentication, HttpServletResponse response) {
        Instant now = clock.instant();
        Duration ttl = Duration.ofMinutes(accessTtlMinutes);
        Duration remaining = Duration.between(now, authentication.expiresAt());

        if (remaining.compareTo(ttl.dividedBy(2)) <= 0) {
            String refreshed = tokenEncoder.createAccessToken(
                    authentication.userId(), authentication.roles(), now, ttl);
            response.addHeader(HttpHeaders.SET_COOKIE,
                    tokenCookieWriter.accessTokenCookie(refreshed, ttl).toString());
        }
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("""
                {
                  "status": 401,
                  "error": "UNAUTHORIZED",
                  "message": "%s"
                }
                """, message));
    }

    private TokenSource extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return new TokenSource(bearerToken.substring(7), false);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    String cookieValue = cookie.getValue();
                    String value = cookieValue.startsWith("Bearer ")
                            ? cookieValue.substring(7)
                            : cookieValue;
                    return new TokenSource(value, true);
                }
            }
        }

        return new TokenSource(null, false);
    }

    private record TokenSource(String value, boolean fromCookie) {
    }
}
