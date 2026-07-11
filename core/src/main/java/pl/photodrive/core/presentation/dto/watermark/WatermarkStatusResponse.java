package pl.photodrive.core.presentation.dto.watermark;

import java.time.Instant;

// updatedAt służy frontowi jako cache-buster podglądu (?v=...); null gdy brak loga.
public record WatermarkStatusResponse(boolean configured, Instant updatedAt) {
}
