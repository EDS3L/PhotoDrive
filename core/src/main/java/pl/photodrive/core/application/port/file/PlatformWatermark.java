package pl.photodrive.core.application.port.file;

import java.time.Instant;

public record PlatformWatermark(byte[] image, Instant updatedAt) {
}
