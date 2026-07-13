package pl.photodrive.core.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

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
    void shouldPassThroughRequestsWithinLimit() throws Exception {
        for (int i = 0; i < LOGIN_ATTEMPTS; i++) {
            MockHttpServletResponse response = call("POST", "/api/auth/login", "10.0.0.1");
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void shouldRejectWithTooManyRequestsAfterExceedingLoginLimit() throws Exception {
        for (int i = 0; i < LOGIN_ATTEMPTS; i++) {
            call("POST", "/api/auth/login", "10.0.0.1");
        }

        MockHttpServletResponse response = call("POST", "/api/auth/login", "10.0.0.1");

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isNotNull();
        assertThat(response.getContentAsString()).contains("TOO_MANY_REQUESTS");
    }

    @Test
    void shouldNotForwardBlockedRequestDownTheChain() throws Exception {
        for (int i = 0; i < LOGIN_ATTEMPTS; i++) {
            call("POST", "/api/auth/login", "10.0.0.1");
        }

        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = request("POST", "/api/auth/login", "10.0.0.1");
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verifyNoInteractions(chain);
    }

    @Test
    void shouldAllowAgainAfterWindowExpires() throws Exception {
        for (int i = 0; i < LOGIN_ATTEMPTS; i++) {
            call("POST", "/api/auth/login", "10.0.0.1");
        }
        assertThat(call("POST", "/api/auth/login", "10.0.0.1").getStatus()).isEqualTo(429);

        now.addAndGet(WINDOW_MINUTES * 60_000L + 1);

        assertThat(call("POST", "/api/auth/login", "10.0.0.1").getStatus()).isEqualTo(200);
    }

    @Test
    void shouldCountLimitsPerClientIp() throws Exception {
        for (int i = 0; i < LOGIN_ATTEMPTS; i++) {
            call("POST", "/api/auth/login", "10.0.0.1");
        }

        assertThat(call("POST", "/api/auth/login", "10.0.0.1").getStatus()).isEqualTo(429);
        assertThat(call("POST", "/api/auth/login", "10.0.0.2").getStatus()).isEqualTo(200);
    }

    @Test
    void shouldCountLoginAndPasswordResetSeparately() throws Exception {
        for (int i = 0; i < PASSWORD_RESET_ATTEMPTS; i++) {
            call("POST", "/api/auth/create/passwordToken/user@photodrive.dev", "10.0.0.1");
        }

        assertThat(call("POST", "/api/auth/create/passwordToken/user@photodrive.dev", "10.0.0.1").getStatus())
                .isEqualTo(429);
        assertThat(call("POST", "/api/auth/login", "10.0.0.1").getStatus()).isEqualTo(200);
    }

    @Test
    void shouldShareLimitBetweenPasswordResetEndpoints() throws Exception {
        for (int i = 0; i < PASSWORD_RESET_ATTEMPTS; i++) {
            call("POST", "/api/auth/create/passwordToken/user@photodrive.dev", "10.0.0.1");
        }

        assertThat(call("POST", "/api/auth/remindPassword", "10.0.0.1").getStatus()).isEqualTo(429);
    }

    @Test
    void shouldIgnoreEndpointsOutsideTheLimitedSet() throws Exception {
        for (int i = 0; i < LOGIN_ATTEMPTS + 5; i++) {
            assertThat(call("POST", "/api/auth/logout", "10.0.0.1").getStatus()).isEqualTo(200);
            assertThat(call("GET", "/api/user/me", "10.0.0.1").getStatus()).isEqualTo(200);
        }
    }

    @Test
    void shouldPreferForwardedClientIpOverProxyAddress() throws Exception {
        for (int i = 0; i < LOGIN_ATTEMPTS; i++) {
            callForwarded("203.0.113.7, 172.18.0.5");
        }

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
