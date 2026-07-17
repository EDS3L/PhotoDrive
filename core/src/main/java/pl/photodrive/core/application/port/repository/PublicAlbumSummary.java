package pl.photodrive.core.application.port.repository;

import java.util.UUID;

/**
 * Wiersz publicznego listingu portfolio — sama metryczka, BEZ materializacji plików albumu.
 * To gorąca ścieżka (każdy gość, zasila zakładki portfolio), więc liczbę widocznych zdjęć
 * liczy baza (COUNT), a nie strumień po załadowanych encjach (B.35).
 */
public record PublicAlbumSummary(UUID albumId,
                                 String name,
                                 String displayName,
                                 int displayOrder,
                                 long visibleCount) {
}
