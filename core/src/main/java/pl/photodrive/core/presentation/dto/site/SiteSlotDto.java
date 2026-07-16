package pl.photodrive.core.presentation.dto.site;

import java.time.Instant;

/** Wiersz panelu admina: każdy slot z enuma, także pusty ({@code configured=false}). */
public record SiteSlotDto(String slot, boolean configured, Instant updatedAt) {
}
