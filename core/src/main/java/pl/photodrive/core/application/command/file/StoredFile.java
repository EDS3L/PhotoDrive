package pl.photodrive.core.application.command.file;

import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.domain.vo.FileName;

/**
 * Plik faktycznie zapisany w albumie: id nadane przez domenę wraz z nazwą, pod którą
 * plik ostatecznie wylądował.
 *
 * <p>Nazwa jest tu istotna, bo domena mogła ją zmienić — przy kolizji
 * {@code makeUniqueFileName} nadaje sufiks ({@code foto.jpg} → {@code foto_1.jpg}).
 * Zwracanie samego {@code FileId} zmuszało kontroler do parowania go z nazwą
 * z ŻĄDANIA, czyli sprzed deduplikacji, i odpowiedź kłamała (B.42).
 */
public record StoredFile(FileId fileId, FileName fileName) {
}
