package pl.photodrive.core.presentation.dto.album;

import java.time.Instant;
import java.util.UUID;

public record FileDto(UUID fileId, String fileName, long sizeBytes, String contentType, Instant uploadedAt, boolean visible, boolean hasWatermark) {
}
