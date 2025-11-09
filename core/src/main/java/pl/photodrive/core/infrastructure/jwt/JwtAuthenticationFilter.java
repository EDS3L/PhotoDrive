package pl.photodrive.core.infrastructure.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import pl.photodrive.core.application.port.TokenDecoder;
import pl.photodrive.core.infrastructure.exception.ExpiredTokenException;
import pl.photodrive.core.infrastructure.exception.InvalidTokenException;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenDecoder tokenDecoder;

    private static final AntPathMatcher PM = new AntPathMatcher();
    private static final String COOKIE_NAME = "pd_at";
    private static final String[] SKIP_PATHS = {"/auth/refresh", "/api/auth/refresh",
            "/auth/login", "/api/auth/login", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"};

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        for (String p : SKIP_PATHS) {
            if (PM.match(p, path)) {
                filterChain.doFilter(request, response);
                return;
            }
        }
        String token = extractToken(request);

        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            var authentication = tokenDecoder.parse(token);
            var authorities = authentication.roles().stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                    .toList();

            var principal = new UsernamePasswordAuthenticationToken(
                    authentication.userId().userId().toString(),
                    null,
                    authorities);

            SecurityContextHolder.getContext().setAuthentication(principal);

        } catch (ExpiredTokenException | InvalidTokenException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }


    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    String cookieValue = cookie.getValue();
                    if (cookieValue.startsWith("Bearer ")) {
                        return cookieValue.substring(7);
                    }
                    return cookieValue;
                }
            }
        }

        return null;
    }
}
