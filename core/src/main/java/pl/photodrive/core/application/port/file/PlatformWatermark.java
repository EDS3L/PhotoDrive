package pl.photodrive.core.application.port.file;

import java.time.Instant;

// updatedAt pełni rolę WERSJI loga — wchodzi do klucza cache watermarkowanych wersji,
// więc podmiana loga automatycznie unieważnia stare wpisy.
public record PlatformWatermark(byte[] image, Instant updatedAt) {
}
