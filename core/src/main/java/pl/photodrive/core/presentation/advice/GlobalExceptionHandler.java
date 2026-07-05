package pl.photodrive.core.presentation.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import pl.photodrive.core.application.exception.AuthenticatedUserException;
import pl.photodrive.core.application.exception.LoginFailedException;
import pl.photodrive.core.application.exception.ApplicationSecurityException;
import pl.photodrive.core.application.exception.StorageOperationException;
import pl.photodrive.core.infrastructure.exception.StorageException;
import pl.photodrive.core.domain.exception.*;
import pl.photodrive.core.domain.exception.DomainSecurityException;
import pl.photodrive.core.domain.exception.AlbumNotFoundException;
import pl.photodrive.core.infrastructure.exception.ExpiredTokenException;
import pl.photodrive.core.infrastructure.exception.InvalidTokenException;
import pl.photodrive.core.presentation.dto.ApiException;

import java.time.Instant;

@RestControllerAdvice
@lombok.extern.slf4j.Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ApiException> userException(UserException ex, HttpServletRequest request) {
        ApiException error = new ApiException("USER_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EmailException.class)
    public ResponseEntity<ApiException> emailException(EmailException ex, HttpServletRequest request) {
        ApiException error = new ApiException("EMAIL_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AlbumNotFoundException.class)
    public ResponseEntity<ApiException> albumNotFoundException(AlbumNotFoundException ex, HttpServletRequest request) {
        ApiException error = new ApiException("ALBUM_NOT_FOUND",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AlbumException.class)
    public ResponseEntity<ApiException> albumException(AlbumException ex, HttpServletRequest request) {
        ApiException error = new ApiException("ALBUM_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(ApplicationSecurityException.class)
    public ResponseEntity<ApiException> securityException(ApplicationSecurityException ex, HttpServletRequest request) {
        ApiException error = new ApiException("SECURITY_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(LoginFailedException.class)
    public ResponseEntity<ApiException> loginFailedException(LoginFailedException ex, HttpServletRequest request) {
        ApiException error = new ApiException("LOGIN_FAILED_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticatedUserException.class)
    public ResponseEntity<ApiException> authenticatedUserException(AuthenticatedUserException ex, HttpServletRequest request) {
        ApiException error = new ApiException("AUTHENTICATION_USER_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }


    @ExceptionHandler(FileException.class)
    public ResponseEntity<ApiException> fileException(FileException ex, HttpServletRequest request) {
        ApiException error = new ApiException("FILE_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ExpiredTokenException.class)
    public ResponseEntity<ApiException> expiredTokenException(ExpiredTokenException ex, HttpServletRequest request) {
        ApiException error = new ApiException("EXPIRED_TOKEN_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiException> invalidTokenException(InvalidTokenException ex, HttpServletRequest request) {
        ApiException error = new ApiException("INVALID_TOKEN_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(PasswordTokenException.class)
    public ResponseEntity<ApiException> PasswordTokenException(PasswordTokenException ex, HttpServletRequest request) {
        ApiException error = new ApiException("PASSWORD_TOKEN_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DomainSecurityException.class)
    public ResponseEntity<ApiException> domainSecurityException(DomainSecurityException ex, HttpServletRequest request) {
        ApiException error = new ApiException("SECURITY_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiException> validationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        ApiException error = new ApiException("VALIDATION_EXCEPTION",
                message,
                java.time.Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiException> illegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        ApiException error = new ApiException("BAD_REQUEST",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(StorageOperationException.class)
    public ResponseEntity<ApiException> storageOperationException(StorageOperationException ex, HttpServletRequest request) {
        log.error("Storage operation failed on {} {}", request.getMethod(), request.getRequestURI(), ex);
        ApiException error = new ApiException("STORAGE_ERROR",
                "Storage operation failed",
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiException> storageException(StorageException ex, HttpServletRequest request) {
        log.error("Storage error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        ApiException error = new ApiException("STORAGE_ERROR",
                "Storage operation failed",
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiException> missingRequestPartException(MissingServletRequestPartException ex, HttpServletRequest request) {
        ApiException error = new ApiException("MISSING_REQUEST_PART",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiException> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        ApiException error = new ApiException("INTERNAL_ERROR",
                "An unexpected error occurred",
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
