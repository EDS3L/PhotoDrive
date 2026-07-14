package pl.photodrive.core.application.exception;

/**
 * Odmowa autoryzacji w warstwie aplikacji (use-case sprawdza rolę/dostęp, zanim wejdzie w domenę).
 * Mapowana na <b>403</b> — odpowiednik
 * {@link pl.photodrive.core.domain.exception.DomainSecurityException} po stronie serwisów.
 */
public class ApplicationSecurityException extends RuntimeException {
    public ApplicationSecurityException(String message) {
        super(message);
    }
}
