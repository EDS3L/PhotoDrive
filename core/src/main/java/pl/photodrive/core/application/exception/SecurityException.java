package pl.photodrive.core.application.exception;

@Deprecated
public class SecurityException extends ApplicationSecurityException {
    public SecurityException(String message) {
        super(message);
    }
}
