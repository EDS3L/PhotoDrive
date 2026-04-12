package pl.photodrive.core.application.exception;

public class ApplicationSecurityException extends RuntimeException {
    public ApplicationSecurityException(String message) {
        super(message);
    }
}
