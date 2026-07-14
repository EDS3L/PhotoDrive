package pl.photodrive.core.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class OriginValidationFilterTest {

    private final OriginValidationFilter filter = new OriginValidationFilter(
            "https://photodrive.dev",
            "https://photodrive.dev");

    @Test
    @DisplayName("Request from the configured origin passes the anti-CSRF check")
    void shouldAllowConfiguredOriginForUnsafeApiRequest() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Origin", "https://photodrive.dev");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        filter.doFilter(request, response, new MockFilterChain());

        // Then
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Localhost is allowed, which keeps local development working")
    void shouldAllowLocalhostOriginForDevelopment() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Origin", "http://localhost:5173");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        filter.doFilter(request, response, new MockFilterChain());

        // Then
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Request from a foreign origin is rejected, which blocks cross-site writes")
    void shouldRejectForeignOriginForUnsafeApiRequest() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Origin", "https://evil.example");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        filter.doFilter(request, response, new MockFilterChain());

        // Then
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("Request without Origin and Referer passes, so non-browser clients still work")
    void shouldAllowUnsafeApiRequestWithoutBrowserOriginHeaders() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        filter.doFilter(request, response, new MockFilterChain());

        // Then
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
