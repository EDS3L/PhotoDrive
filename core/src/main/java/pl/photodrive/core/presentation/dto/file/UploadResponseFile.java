package pl.photodrive.core.presentation.dto.file;

/**
 * Pojedynczy plik w odpowiedzi uploadu. Pole nazywa się {@code fileId}, a nie {@code id} —
 * to ta sama konwencja co w {@code FileDto} po 6.1 (identyfikator pliku zawsze pod jedną nazwą).
 */
public record UploadResponseFile(String fileId, String fileName) {
}
