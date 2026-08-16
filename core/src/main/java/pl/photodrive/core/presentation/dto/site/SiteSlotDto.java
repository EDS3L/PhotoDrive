package pl.photodrive.core.presentation.dto.site;

import java.time.Instant;

public record SiteSlotDto(String slot, boolean configured, Instant updatedAt) {
}
