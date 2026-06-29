package pl.photodrive.core.infrastructure.security;

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
    void shouldAllowConfiguredOriginForUnsafeApiRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Origin", "https://photodrive.dev");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldAllowLocalhostOriginForDevelopment() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Origin", "http://localhost:5173");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldRejectForeignOriginForUnsafeApiRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Origin", "https://evil.example");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void shouldAllowUnsafeApiRequestWithoutBrowserOriginHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
