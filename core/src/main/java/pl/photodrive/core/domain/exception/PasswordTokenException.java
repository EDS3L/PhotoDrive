package pl.photodrive.core.domain.exception;

public class PasswordTokenException extends RuntimeException {
    public PasswordTokenException(String message) {
        super(message);
    }
}
