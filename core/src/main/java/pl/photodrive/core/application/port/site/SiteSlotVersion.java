package pl.photodrive.core.application.port.site;

import java.time.Instant;

public record SiteSlotVersion(SiteSlot slot, Instant updatedAt) {
}
