package pl.photodrive.core.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.BDDMockito.*;

class RateLimitFilterTest {

    private static final int LOGIN_ATTEMPTS = 3;
    private static final int PASSWORD_RESET_ATTEMPTS = 2;
    private static final int WINDOW_MINUTES = 15;

    private AtomicLong now;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        now = new AtomicLong(0);
        filter = new RateLimitFilter(WINDOW_MINUTES, LOGIN_ATTEMPTS, PASSWORD_RESET_ATTEMPTS, now::get);
    }

    @Test
    @DisplayName("Requests within the limit pass through")
    void shouldPassThroughRequestsWithinLimit() throws Exception {
        // When / Then - every attempt within the limit passes
        for (int i = 0; i < LOGIN_ATTEMPTS; i++) {
            MockHttpServletResponse response = call("POST", "/api/auth/login", "10.0.0.1");
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("Exceeding the login limit returns 429 with a Retry-After hint")
    void shouldRejectWithTooManyRequestsAfterExceedingLoginLimit() throws Exception {
        // Given - the limit is already exhausted
        for (int i = 0; i < LOGIN_ATTEMPTS; i++) {
            call("POST", "/api/auth/login", "10.0.0.1");
        }

        // When
        MockHttpServletResponse response = call("POST", "/api/auth/login", "10.0.0.1");

        // Then
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isNotNull();
        assertThat(response.getContentAsString()).contains("TOO_MANY_REQUESTS");
    }

    @Test
    @DisplayName("Blocked request never reaches the database or BCrypt")
    void shouldNotForwardBlockedRequestDownTheChain() throws Exception {
        // Given - the limit is already exhausted
        for (int i = 0; i < LOGIN_ATTEMPTS; i++) {
            call("POST", "/api/auth/login", "10.0.0.1");
        }
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = request("POST", "/api/auth/login", "10.0.0.1");

        // When
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // Then
        then(chain).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Limit resets once the time window has passed")
    void shouldAllowAgainAfterWindowExpires() throws Exception {
        // Given - the limit is exhausted and the next attempt is already refused
        for (int i = 0; i < LOGIN_ATTEMPTS; i++) {
            call("POST", "/api/auth/login", "10.0.0.1");
        }
        assertThat(call("POST", "/api/auth/login", "10.0.0.1").getStatus()).isEqualTo(429);

        // When - the time window passes
        now.addAndGet(WINDOW_MINUTES * 60_000L + 1);

        // Then
        assertThat(call("POST", "/api/auth/login", "10.0.0.1").getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Limits are counted per client IP, so one attacker does not lock out everyone")
    void shouldCountLimitsPerClientIp() throws Exception {
        // Given - the limit is exhausted for the first IP
        for (int i = 0; i < LOGIN_ATTEMPTS; i++) {
            call("POST", "/api/auth/login", "10.0.0.1");
        }

        // When / Then - only that IP is blocked
        assertThat(call("POST", "/api/auth/login", "10.0.0.1").getStatus()).isEqualTo(429);
        assertThat(call("POST", "/api/auth/login", "10.0.0.2").getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Login and password reset have separate budgets")
    void shouldCountLoginAndPasswordResetSeparately() throws Exception {
        // Given - the password reset budget is exhausted
        for (int i = 0; i < PASSWORD_RESET_ATTEMPTS; i++) {
            call("POST", "/api/auth/create/passwordToken/user@photodrive.dev", "10.0.0.1");
        }

        // When / Then - reset is blocked while login still works
        assertThat(call("POST", "/api/auth/create/passwordToken/user@photodrive.dev", "10.0.0.1").getStatus())
                .isEqualTo(429);
        assertThat(call("POST", "/api/auth/login", "10.0.0.1").getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Both reset endpoints share one budget, so a new code cannot refresh the guessing limit")
    void shouldShareLimitBetweenPasswordResetEndpoints() throws Exception {
        // Given - the budget is exhausted on the code-generation endpoint
        for (int i = 0; i < PASSWORD_RESET_ATTEMPTS; i++) {
            call("POST", "/api/auth/create/passwordToken/user@photodrive.dev", "10.0.0.1");
        }

        // When / Then - the other reset endpoint shares the same budget
        assertThat(call("POST", "/api/auth/remindPassword", "10.0.0.1").getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("Endpoints outside the protected set are not throttled")
    void shouldIgnoreEndpointsOutsideTheLimitedSet() throws Exception {
        // When / Then - far more requests than the limit, and they all pass
        for (int i = 0; i < LOGIN_ATTEMPTS + 5; i++) {
            assertThat(call("POST", "/api/auth/logout", "10.0.0.1").getStatus()).isEqualTo(200);
            assertThat(call("GET", "/api/user/me", "10.0.0.1").getStatus()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("Client IP is taken from X-Forwarded-For, so every user behind the proxy is not one bucket")
    void shouldPreferForwardedClientIpOverProxyAddress() throws Exception {
        // Given - the limit is exhausted by one client behind the proxy
        for (int i = 0; i < LOGIN_ATTEMPTS; i++) {
            callForwarded("203.0.113.7, 172.18.0.5");
        }

        // When / Then - the client is blocked, not the proxy
        assertThat(callForwarded("203.0.113.7, 172.18.0.5").getStatus()).isEqualTo(429);
        assertThat(callForwarded("203.0.113.8, 172.18.0.5").getStatus()).isEqualTo(200);
    }

    private MockHttpServletResponse callForwarded(String forwardedFor) throws ServletException, IOException {
        MockHttpServletRequest request = request("POST", "/api/auth/login", "172.18.0.5");
        request.addHeader("X-Forwarded-For", forwardedFor);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private MockHttpServletResponse call(String method, String uri, String remoteAddr)
            throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request(method, uri, remoteAddr), response, new MockFilterChain());
        return response;
    }

    private MockHttpServletRequest request(String method, String uri, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
