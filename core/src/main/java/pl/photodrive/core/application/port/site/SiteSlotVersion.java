package pl.photodrive.core.application.port.site;

import java.time.Instant;

/** Wpis listingu slotów — sama wersja, bez bajtów obrazka (listing nie może ciągnąć BLOB-ów). */
public record SiteSlotVersion(SiteSlot slot, Instant updatedAt) {
}
