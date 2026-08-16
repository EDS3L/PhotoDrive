package pl.photodrive.core.presentation.dto.watermark;

import java.time.Instant;

public record WatermarkStatusResponse(boolean configured, Instant updatedAt) {
}
