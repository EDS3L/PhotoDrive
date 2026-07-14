package pl.photodrive.core.domain.exception;

/**
 * Odmowa autoryzacji w domenie: użytkownik nie ma prawa wykonać operacji (rola, własność albumu).
 * Mapowana na <b>403</b>.
 *
 * <p>Świadomie różna od {@link AlbumException} / {@link UserException}, które oznaczają
 * <b>złamaną regułę biznesową</b> (400): „TTD w przeszłości", „plik o tej nazwie już istnieje".
 * Rozróżnienie jest istotne — 400 mówi „popraw dane i spróbuj ponownie", a 403 „nie próbuj,
 * to nie twoje". Wcześniej odmowy leciały jako 400 i były nieodróżnialne od literówki (A10).
 */
public class DomainSecurityException extends RuntimeException {
    public DomainSecurityException(String message) {
        super(message);
    }
}
