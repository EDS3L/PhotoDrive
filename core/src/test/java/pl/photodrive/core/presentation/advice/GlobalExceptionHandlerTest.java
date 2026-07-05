package pl.photodrive.core.presentation.advice;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import pl.photodrive.core.application.exception.ApplicationSecurityException;
import pl.photodrive.core.application.exception.LoginFailedException;
import pl.photodrive.core.domain.exception.*;
import pl.photodrive.core.infrastructure.exception.ExpiredTokenException;
import pl.photodrive.core.infrastructure.exception.InvalidTokenException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for GlobalExceptionHandler — no Spring context needed
 * since the handler is a POJO with simple handler methods.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");

    @Test
    void userException_shouldReturn400WithUserExceptionCode() {
        var response = handler.userException(new UserException("User error"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("USER_EXCEPTION");
    }

    @Test
    void emailException_shouldReturn400WithEmailExceptionCode() {
        var response = handler.emailException(new EmailException("Email error"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorCode()).isEqualTo("EMAIL_EXCEPTION");
    }

    @Test
    void albumException_shouldReturn400WithAlbumExceptionCode() {
        var response = handler.albumException(new AlbumException("Album error"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorCode()).isEqualTo("ALBUM_EXCEPTION");
    }

    @Test
    void applicationSecurityException_shouldReturn403WithSecurityCode() {
        var response = handler.securityException(new ApplicationSecurityException("Forbidden!"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().errorCode()).isEqualTo("SECURITY_EXCEPTION");
    }

    @Test
    void domainSecurityException_shouldReturn403WithSecurityCode() {
        var response = handler.domainSecurityException(new DomainSecurityException("Domain forbidden!"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().errorCode()).isEqualTo("SECURITY_EXCEPTION");
    }

    @Test
    void loginFailedException_shouldReturn401() {
        var response = handler.loginFailedException(new LoginFailedException("Login failed!"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().errorCode()).isEqualTo("LOGIN_FAILED_EXCEPTION");
    }

    @Test
    void expiredTokenException_shouldReturn401() {
        var response = handler.expiredTokenException(new ExpiredTokenException("Token expired!"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().errorCode()).isEqualTo("EXPIRED_TOKEN_EXCEPTION");
    }

    @Test
    void invalidTokenException_shouldReturn401() {
        var response = handler.invalidTokenException(new InvalidTokenException("Token invalid!"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().errorCode()).isEqualTo("INVALID_TOKEN_EXCEPTION");
    }

    @Test
    void passwordTokenException_shouldReturn400() {
        var response = handler.PasswordTokenException(new PasswordTokenException("Token error!"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorCode()).isEqualTo("PASSWORD_TOKEN_EXCEPTION");
    }

    @Test
    void fileException_shouldReturn400WithFileExceptionCode() {
        var response = handler.fileException(new FileException("File error!"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorCode()).isEqualTo("FILE_EXCEPTION");
    }

    @Test
    void unexpectedException_shouldReturn500WithInternalErrorCode() {
        var response = handler.handleUnexpectedException(new RuntimeException("Unexpected!"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void methodArgumentNotValid_shouldReturn400WithValidationExceptionCode() throws Exception {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "must not be blank"));

        // MethodArgumentNotValidException needs reflective construction in Spring 6
        var ex = new MethodArgumentNotValidException(null, bindingResult);
        var response = handler.validationException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errorCode()).isEqualTo("VALIDATION_EXCEPTION");
    }
}
