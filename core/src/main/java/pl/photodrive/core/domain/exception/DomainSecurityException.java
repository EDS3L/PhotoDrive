package pl.photodrive.core.domain.exception;

/**
 * Odmowa autoryzacji w domenie (rola, własność albumu) — mapowana na 403.
 * Różna od {@link AlbumException}/{@link UserException}: te oznaczają złamaną regułę biznesową (400).
 */
public class DomainSecurityException extends RuntimeException {
    public DomainSecurityException(String message) {
        super(message);
    }
}
