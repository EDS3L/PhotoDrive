package pl.photodrive.core.presentation.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.photodrive.core.application.exception.AuthenticatedUserException;
import pl.photodrive.core.application.exception.LoginFailedException;
import pl.photodrive.core.application.exception.SecurityException;
import pl.photodrive.core.domain.exception.*;
import pl.photodrive.core.infrastructure.exception.ExpiredTokenException;
import pl.photodrive.core.infrastructure.exception.InvalidTokenException;
import pl.photodrive.core.presentation.dto.ApiException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ApiException> userException(UserException ex, HttpServletRequest request) {
        ApiException error = new ApiException("USER_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(EmailException.class)
    public ResponseEntity<ApiException> emailException(EmailException ex, HttpServletRequest request) {
        ApiException error = new ApiException("EMAIL_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(AlbumException.class)
    public ResponseEntity<ApiException> albumException(AlbumException ex, HttpServletRequest request) {
        ApiException error = new ApiException("ALBUM_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.NOT_ACCEPTABLE);
    }


    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiException> securityException(SecurityException ex, HttpServletRequest request) {
        ApiException error = new ApiException("SECURITY_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(LoginFailedException.class)
    public ResponseEntity<ApiException> loginFailedException(LoginFailedException ex, HttpServletRequest request) {
        ApiException error = new ApiException("LOGIN_FAILED_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(AuthenticatedUserException.class)
    public ResponseEntity<ApiException> authenticatedUserException(AuthenticatedUserException ex, HttpServletRequest request) {
        ApiException error = new ApiException("AUTHENTICATION_USER_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.NOT_ACCEPTABLE);
    }


    @ExceptionHandler(FileException.class)
    public ResponseEntity<ApiException> fileException(FileException ex, HttpServletRequest request) {
        ApiException error = new ApiException("FILE_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(ExpiredTokenException.class)
    public ResponseEntity<ApiException> expiredTokenException(ExpiredTokenException ex, HttpServletRequest request) {
        ApiException error = new ApiException("EXPIRED_TOKEN_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiException> invalidTokenException(InvalidTokenException ex, HttpServletRequest request) {
        ApiException error = new ApiException("INVALID_TOKEN_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(PasswordTokenException.class)
    public ResponseEntity<ApiException> PasswordTokenException(PasswordTokenException ex, HttpServletRequest request) {
        ApiException error = new ApiException("PASSWORD_TOKEN_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.NOT_ACCEPTABLE);
    }

}
