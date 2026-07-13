package pl.photodrive.core.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Ogranicza liczbę prób na wrażliwych endpointach uwierzytelniania (okno czasowe per IP + endpoint).
 * Chroni przed zgadywaniem hasła, zgadywaniem kodu autoryzacji i zasypywaniem skrzynek mailem resetującym.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REMIND_PASSWORD_PATH = "/api/auth/remindPassword";
    private static final String CREATE_TOKEN_PATH = "/api/auth/create/passwordToken/**";

    /** Powyżej tego rozmiaru mapa jest czyszczona z wygasłych okien — limit zużycia pamięci. */
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final LongSupplier clock;
    private final long windowMillis;
    private final int loginAttempts;
    private final int passwordResetAttempts;

    @Autowired
    public RateLimitFilter(@Value("${app.rate-limit.window-minutes:15}") int windowMinutes,
                           @Value("${app.rate-limit.login-attempts:10}") int loginAttempts,
                           @Value("${app.rate-limit.password-reset-attempts:5}") int passwordResetAttempts) {
        this(windowMinutes, loginAttempts, passwordResetAttempts, System::currentTimeMillis);
    }

    RateLimitFilter(int windowMinutes, int loginAttempts, int passwordResetAttempts, LongSupplier clock) {
        this.windowMillis = Duration.ofMinutes(windowMinutes).toMillis();
        this.loginAttempts = loginAttempts;
        this.passwordResetAttempts = passwordResetAttempts;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return limitFor(request).isEmpty();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Limit limit = limitFor(request).orElseThrow();
        String key = limit.name() + "|" + clientIp(request);

        long now = clock.getAsLong();
        Window window = register(key, now);

        if (window.count > limit.maxAttempts()) {
            reject(response, Math.max(1, (window.resetAt - now) / 1000));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Window register(String key, long now) {
        if (windows.size() > MAX_TRACKED_KEYS) {
            windows.values().removeIf(window -> now >= window.resetAt);
        }

        return windows.compute(key, (ignored, existing) -> {
            if (existing == null || now >= existing.resetAt) {
                return new Window(now + windowMillis);
            }
            existing.count++;
            return existing;
        });
    }

    private void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
                {
                  "status": 429,
                  "error": "TOO_MANY_REQUESTS",
                  "message": "Zbyt wiele prób. Spróbuj ponownie za %d min."
                }
                """.formatted(Math.max(1, retryAfterSeconds / 60)));
    }

    private Optional<Limit> limitFor(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return Optional.empty();
        }

        String path = request.getRequestURI();
        if (LOGIN_PATH.equals(path)) {
            return Optional.of(new Limit("login", loginAttempts));
        }
        if (REMIND_PASSWORD_PATH.equals(path) || PATH_MATCHER.match(CREATE_TOKEN_PATH, path)) {
            return Optional.of(new Limit("password-reset", passwordResetAttempts));
        }
        return Optional.empty();
    }

    /**
     * Za odwrotnym proxy (Traefik) X-Forwarded-For jest nadpisywany dla ruchu z zewnątrz,
     * więc pierwszy wpis to realny klient, a nie wartość podstawiona przez atakującego.
     */
    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim().toLowerCase(Locale.ROOT);
        }
        return request.getRemoteAddr();
    }

    private record Limit(String name, int maxAttempts) {
    }

    private static final class Window {
        private final long resetAt;
        private int count = 1;

        private Window(long resetAt) {
            this.resetAt = resetAt;
        }
    }
}
