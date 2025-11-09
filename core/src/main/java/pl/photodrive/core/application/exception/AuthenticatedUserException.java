package pl.photodrive.core.application.exception;

public class AuthenticatedUserException extends RuntimeException {
    public AuthenticatedUserException(String message) {
        super(message);
    }
}
