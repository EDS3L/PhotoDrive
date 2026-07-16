package pl.photodrive.core.application.port.site;

import java.time.Instant;

/** Zdjęcie slotu; {@code updatedAt} służy jako wersja do unieważniania cache przeglądarki. */
public record SiteSlotImage(byte[] image, Instant updatedAt) {
}
