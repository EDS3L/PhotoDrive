package pl.photodrive.core.presentation.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.photodrive.core.domain.exception.EmailException;
import pl.photodrive.core.domain.exception.UserException;
import pl.photodrive.core.presentation.dto.ApiException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ApiException> userException(UserException ex, HttpServletRequest request) {
        ApiException error = new ApiException(
                "USER_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(EmailException.class)
    public ResponseEntity<ApiException> EmailException(EmailException ex, HttpServletRequest request) {
        ApiException error = new ApiException(
                "EMAIL_EXCEPTION",
                ex.getMessage(),
                Instant.now(),
                request.getRequestURI());
        return new ResponseEntity<>(error, HttpStatus.NOT_ACCEPTABLE);
    }
}
