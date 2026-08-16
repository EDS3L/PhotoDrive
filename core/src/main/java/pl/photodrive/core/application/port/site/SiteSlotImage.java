package pl.photodrive.core.application.port.site;

import java.time.Instant;

public record SiteSlotImage(byte[] image, Instant updatedAt) {
}
