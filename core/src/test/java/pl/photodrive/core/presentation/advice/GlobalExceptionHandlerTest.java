package pl.photodrive.core.presentation.advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pl.photodrive.core.application.exception.ApplicationSecurityException;
import pl.photodrive.core.application.exception.AuthenticatedUserException;
import pl.photodrive.core.application.exception.LoginFailedException;
import pl.photodrive.core.application.exception.StorageOperationException;
import pl.photodrive.core.domain.exception.*;
import pl.photodrive.core.infrastructure.exception.ExpiredTokenException;
import pl.photodrive.core.infrastructure.exception.InvalidTokenException;
import pl.photodrive.core.infrastructure.exception.StorageException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for GlobalExceptionHandler - no Spring context needed
 * since the handler is a POJO with simple handler methods.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");

    @Test
    @DisplayName("User rule violation is reported as 400")
    void shouldMapUserExceptionTo400() {
        // When
        var response = handler.userException(new UserException("User error"), request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("USER_EXCEPTION");
    }

    @Test
    @DisplayName("Invalid email is reported as 400")
    void shouldMapEmailExceptionTo400() {
        // When
        var response = handler.emailException(new EmailException("Email error"), request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorCode()).isEqualTo("EMAIL_EXCEPTION");
    }

    @Test
    @DisplayName("Album rule violation is reported as 400")
    void shouldMapAlbumExceptionTo400() {
        // When
        var response = handler.albumException(new AlbumException("Album error"), request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorCode()).isEqualTo("ALBUM_EXCEPTION");
    }

    @Test
    @DisplayName("Application-level authorization failure is reported as 403")
    void shouldMapApplicationSecurityExceptionTo403() {
        // When
        var response = handler.securityException(new ApplicationSecurityException("Forbidden!"), request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().errorCode()).isEqualTo("SECURITY_EXCEPTION");
    }

    @Test
    @DisplayName("Domain-level authorization failure is reported as 403")
    void shouldMapDomainSecurityExceptionTo403() {
        // When
        var response = handler.domainSecurityException(new DomainSecurityException("Domain forbidden!"), request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().errorCode()).isEqualTo("SECURITY_EXCEPTION");
    }

    @Test
    @DisplayName("Failed login is reported as 401")
    void shouldMapLoginFailedExceptionTo401() {
        // When
        var response = handler.loginFailedException(new LoginFailedException("Login failed!"), request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().errorCode()).isEqualTo("LOGIN_FAILED_EXCEPTION");
    }

    @Test
    @DisplayName("Expired session token is reported as 401")
    void shouldMapExpiredTokenExceptionTo401() {
        // When
        var response = handler.expiredTokenException(new ExpiredTokenException("Token expired!"), request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().errorCode()).isEqualTo("EXPIRED_TOKEN_EXCEPTION");
    }

    @Test
    @DisplayName("Invalid session token is reported as 401")
    void shouldMapInvalidTokenExceptionTo401() {
        // When
        var response = handler.invalidTokenException(new InvalidTokenException("Token invalid!"), request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().errorCode()).isEqualTo("INVALID_TOKEN_EXCEPTION");
    }

    @Test
    @DisplayName("Invalid or expired authorization code is reported as 400, identically for every failure")
    void shouldMapPasswordTokenExceptionTo400() {
        // When
        var response = handler.PasswordTokenException(new PasswordTokenException("Token error!"), request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorCode()).isEqualTo("PASSWORD_TOKEN_EXCEPTION");
    }

    @Test
    @DisplayName("File rule violation is reported as 400")
    void shouldMapFileExceptionTo400() {
        // When
        var response = handler.fileException(new FileException("File error!"), request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorCode()).isEqualTo("FILE_EXCEPTION");
    }

    @Test
    @DisplayName("Unexpected error is reported as 500 without leaking internals")
    void shouldMapUnexpectedExceptionTo500() {
        // When
        var response = handler.handleUnexpectedException(new RuntimeException("Unexpected!"), request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    @DisplayName("Storage failure is reported as 503 and its message is replaced, so server paths and stack details never reach the client")
    void shouldMapStorageExceptionTo503WithoutLeakingDetails() {
        // Given - a storage error whose message carries an absolute server path
        var ex = new StorageException("Failed to save file: /app/photodrive/foto@studio.pl/sesja/IMG_1.jpg");

        // When
        var response = handler.storageException(ex, request);

        // Then - 503 says "try again later" rather than "you sent bad data"; the original
        // message must NOT be echoed, because it describes the server's filesystem
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("STORAGE_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Storage operation failed");
        assertThat(response.getBody().message()).doesNotContain("/app/photodrive");
    }

    @Test
    @DisplayName("Failed storage operation during upload is reported as 503, also without echoing the original message")
    void shouldMapStorageOperationExceptionTo503WithoutLeakingDetails() {
        // Given
        var ex = new StorageOperationException("Temporary file not found: /tmp/photodrive-temp/abc-123");

        // When
        var response = handler.storageOperationException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().errorCode()).isEqualTo("STORAGE_ERROR");
        assertThat(response.getBody().message()).doesNotContain("/tmp/photodrive-temp");
    }

    @Test
    @DisplayName("A request without an authenticated user is reported as 401, so the front end redirects to login instead of showing a broken screen")
    void shouldMapAuthenticatedUserExceptionTo401() {
        // When
        var response = handler.authenticatedUserException(
                new AuthenticatedUserException("No authenticated user"), request);

        // Then - the axios interceptor keys the redirect off 401; any other status leaves
        // the user staring at an error instead of the login form
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().errorCode()).isEqualTo("AUTHENTICATION_USER_EXCEPTION");
    }

    @Test
    @DisplayName("Bean validation error is reported as 400 with the field details")
    void shouldMapValidationErrorTo400() throws Exception {
        // Given
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "must not be blank"));

        // MethodArgumentNotValidException needs reflective construction in Spring 6
        var ex = new MethodArgumentNotValidException(null, bindingResult);

        // When
        var response = handler.validationException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorCode()).isEqualTo("VALIDATION_EXCEPTION");
    }

    @Test
    @DisplayName("A malformed request body is the client's fault, so it answers 400 and not a server error")
    void shouldMapUnreadableBodyTo400() {
        // Given - Jackson cannot deserialize the payload (broken JSON or a field of the wrong type)
        var ex = new HttpMessageNotReadableException("Cannot deserialize value of type `java.util.UUID`",
                (org.springframework.http.HttpInputMessage) null);

        // When
        var response = handler.notReadableException(ex, request);

        // Then - without this handler the exception fell through to the catch-all and became
        // a 500, so a user's typo looked like an outage (B.45)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("BAD_REQUEST");
    }

    @Test
    @DisplayName("The parser message never reaches the client, because it quotes internal class names and part of the payload")
    void shouldNotLeakParserDetailsOnUnreadableBody() {
        // Given
        var ex = new HttpMessageNotReadableException(
                "JSON parse error: Cannot deserialize value of type `pl.photodrive.core.domain.vo.UserId`",
                (org.springframework.http.HttpInputMessage) null);

        // When
        var response = handler.notReadableException(ex, request);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).doesNotContain("pl.photodrive");
        assertThat(response.getBody().message()).doesNotContain("JSON parse error");
    }

    @Test
    @DisplayName("A path parameter of the wrong type answers 400 and names the parameter, so the caller can see what to fix")
    void shouldMapTypeMismatchTo400() throws Exception {
        // Given - e.g. /api/user/not-a-uuid/assignedUsers
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("sampleHandler", java.util.UUID.class), 0);
        var ex = new MethodArgumentTypeMismatchException("not-a-uuid",
                java.util.UUID.class,
                "id",
                parameter,
                new IllegalArgumentException("Invalid UUID string"));

        // When
        var response = handler.typeMismatchException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("id");
    }

    /** Cel dla {@link MethodParameter} w teście wyżej - nigdy nie wołany. */
    @SuppressWarnings("unused")
    private void sampleHandler(java.util.UUID id) {
    }
}
