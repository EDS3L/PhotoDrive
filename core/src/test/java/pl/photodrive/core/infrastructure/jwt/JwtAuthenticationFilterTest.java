package pl.photodrive.core.infrastructure.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import pl.photodrive.core.application.port.token.TokenDecoder;
import pl.photodrive.core.application.port.token.TokenEncoder;
import pl.photodrive.core.application.port.user.AuthenticatedUser;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.vo.UserId;
import pl.photodrive.core.presentation.web.cookie.TokenCookieWriter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.BDDMockito.*;

class JwtAuthenticationFilterTest {

    private static final long TTL_MINUTES = 60L;

    private TokenDecoder tokenDecoder;
    private TokenEncoder tokenEncoder;
    private TokenCookieWriter cookieWriter;
    private JwtAuthenticationFilter filter;

    private final Instant now = Instant.parse("2026-01-01T12:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final UserId userId = new UserId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        tokenDecoder = mock(TokenDecoder.class);
        tokenEncoder = mock(TokenEncoder.class);
        cookieWriter = mock(TokenCookieWriter.class);
        filter = new JwtAuthenticationFilter(tokenDecoder, tokenEncoder, cookieWriter, clock);
        ReflectionTestUtils.setField(filter, "accessTtlMinutes", TTL_MINUTES);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Session cookie is renewed once the token is past half of its life, so activity extends the session")
    void shouldRenewCookieWhenTokenIsPastHalfLife() throws Exception {
        // Given
        // 20 min remaining < 30 min threshold -> renew
        stubDecode(now.plus(Duration.ofMinutes(20)));
        given(tokenEncoder.createAccessToken(any(), any(), any(), any())).willReturn("fresh.jwt");
        given(cookieWriter.accessTokenCookie(any(), any())).willReturn(ResponseCookie.from("pd_at", "fresh.jwt").build());

        // When
        MockHttpServletResponse response = invokeWithCookie("old.jwt");

        // Then
        then(tokenEncoder).should().createAccessToken(any(), any(), any(), any());
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
                .anyMatch(h -> h.contains("pd_at="));
    }

    @Test
    @DisplayName("Fresh token is not renewed on every request")
    void shouldNotRenewCookieWhenTokenHasPlentyOfLife() throws Exception {
        // Given
        // 50 min remaining > 30 min threshold -> no renew
        stubDecode(now.plus(Duration.ofMinutes(50)));

        // When
        MockHttpServletResponse response = invokeWithCookie("old.jwt");

        // Then
        then(tokenEncoder).should(never()).createAccessToken(any(), any(), any(), any());
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
    }

    @Test
    @DisplayName("Bearer tokens are not renewed; renewal applies to cookie sessions only")
    void shouldNotRenewCookieForBearerTokens() throws Exception {
        // Given
        // near expiry, but token comes from Authorization header (API client) -> no renew
        stubDecode(now.plus(Duration.ofMinutes(5)));

        MockHttpServletRequest request = baseRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer api.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        filter.doFilter(request, response, mock(FilterChain.class));

        // Then
        then(tokenEncoder).should(never()).createAccessToken(any(), any(), any(), any());
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
    }

    private void stubDecode(Instant expiresAt) {
        given(tokenDecoder.parse(anyString())).willReturn(new AuthenticatedUser(userId, Set.of(Role.ADMIN), expiresAt));
    }

    private MockHttpServletRequest baseRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/album/all");
        return request;
    }

    private MockHttpServletResponse invokeWithCookie(String token) throws Exception {
        MockHttpServletRequest request = baseRequest();
        request.setCookies(new Cookie("pd_at", token));
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, mock(FilterChain.class));
        return response;
    }
}
