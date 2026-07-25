package pl.photodrive.core.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class OriginValidationFilterTest {

    /** Konfiguracja deweloperska: furtka na localhost otwarta (jak lokalny `bootRun`). */
    private final OriginValidationFilter filter = new OriginValidationFilter(
            "https://photodrive.dev",
            "https://photodrive.dev",
            true);

    /** Konfiguracja produkcyjna: `app.csrf.allow-localhost-origins=false` (`application-prod.yml`). */
    private final OriginValidationFilter productionFilter = new OriginValidationFilter(
            "https://photodrive.dev",
            "https://photodrive.dev",
            false);

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
    @DisplayName("In production localhost is just another foreign origin, so the dev gate is closed there")
    void shouldRejectLocalhostOriginWhenLocalhostGateIsDisabled() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Origin", "http://localhost:5173");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        productionFilter.doFilter(request, response, new MockFilterChain());

        // Then
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("Closing the localhost gate does not lock out the real front-end origin")
    void shouldStillAllowConfiguredOriginWhenLocalhostGateIsDisabled() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Origin", "https://photodrive.dev");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        productionFilter.doFilter(request, response, new MockFilterChain());

        // Then
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("The loopback address is treated exactly like localhost, so closing one closes the other")
    void shouldRejectLoopbackAddressWhenLocalhostGateIsDisabled() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Origin", "http://127.0.0.1:3000");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        productionFilter.doFilter(request, response, new MockFilterChain());

        // Then
        assertThat(response.getStatus()).isEqualTo(403);
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

    @Test
    @DisplayName("A malformed Origin is rejected rather than treated as a missing one, so garbage in the header is not a way around the check")
    void shouldRejectMalformedOriginInsteadOfTreatingItAsAbsent() throws Exception {
        // Given - an Origin that cannot be parsed into scheme + host
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Origin", "not-a-valid-origin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        filter.doFilter(request, response, new MockFilterChain());

        // Then - the "no headers at all" branch exists for non-browser clients; falling into
        // it on unparsable input would turn a bad Origin into a free pass
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("Referer stands in when Origin is absent, so a browser that sends only Referer is still checked")
    void shouldFallBackToRefererWhenOriginMissing() throws Exception {
        // Given - no Origin, but a Referer pointing at a foreign site
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/album/admin/create");
        request.addHeader("Referer", "https://zly-serwis.example/atak.html");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        filter.doFilter(request, response, new MockFilterChain());

        // Then
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("Referer from the real front end passes, because the fallback compares origins and not whole URLs")
    void shouldAllowRefererFromConfiguredOrigin() throws Exception {
        // Given - a Referer carries the full page URL, not a bare origin
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/album/admin/create");
        request.addHeader("Referer", "https://photodrive.dev/admin/albums?tab=portfolio");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        filter.doFilter(request, response, new MockFilterChain());

        // Then - comparing the raw string would reject every real request from the app
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Origin matching ignores letter case, so an upper-case host from the browser is not read as a foreign site")
    void shouldMatchOriginCaseInsensitively() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Origin", "HTTPS://PhotoDrive.DEV");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        filter.doFilter(request, response, new MockFilterChain());

        // Then
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("The localhost gate covers http and https only, so a non-web scheme pointing at localhost is not trusted even in development")
    void shouldNotTrustNonHttpSchemeOnLocalhost() throws Exception {
        // Given - dev configuration, where plain http://localhost WOULD be accepted
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Origin", "ftp://localhost:5173");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        filter.doFilter(request, response, new MockFilterChain());

        // Then - the gate is for the local front end, not for anything that merely says "localhost"
        assertThat(response.getStatus()).isEqualTo(403);
    }
}
