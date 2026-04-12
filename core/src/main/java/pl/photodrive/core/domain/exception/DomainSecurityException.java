package pl.photodrive.core.domain.exception;

public class DomainSecurityException extends RuntimeException {
    public DomainSecurityException(String message) {
        super(message);
    }
}
