package pl.photodrive.core.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class OriginValidationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private final Set<String> allowedOrigins;

    public OriginValidationFilter(@Value("${app.base-url:http://localhost:8080}") String baseUrl,
                                  @Value("${app.csrf.allowed-origins:https://photodrive.dev}") String configuredOrigins) {
        this.allowedOrigins = Stream.concat(Stream.of(baseUrl), Arrays.stream(configuredOrigins.split(",")))
                .map(String::trim)
                .map(this::normalizeOrigin)
                .flatMap(Optional::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return SAFE_METHODS.contains(request.getMethod().toUpperCase(Locale.ROOT))
                || !PATH_MATCHER.match("/api/**", request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");

        if ((origin == null || origin.isBlank()) && (referer == null || referer.isBlank())) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<String> requestOrigin = normalizeOrigin(origin);
        if (requestOrigin.isEmpty()) {
            requestOrigin = normalizeOrigin(referer);
        }

        if (requestOrigin.isEmpty() || !isAllowed(requestOrigin.get())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                    {
                      "status": 403,
                      "error": "FORBIDDEN",
                      "message": "Invalid request origin."
                    }
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(String origin) {
        return allowedOrigins.contains(origin) || isLocalDevelopmentOrigin(origin);
    }

    private boolean isLocalDevelopmentOrigin(String origin) {
        try {
            URI uri = URI.create(origin);
            String host = uri.getHost();
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Optional<String> normalizeOrigin(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            URI uri = URI.create(value);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return Optional.empty();
            }

            StringBuilder origin = new StringBuilder()
                    .append(uri.getScheme().toLowerCase(Locale.ROOT))
                    .append("://")
                    .append(uri.getHost().toLowerCase(Locale.ROOT));

            if (uri.getPort() != -1) {
                origin.append(":").append(uri.getPort());
            }

            return Optional.of(origin.toString());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
