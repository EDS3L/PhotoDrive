package pl.photodrive.core.presentation.dto.site;

/** Wpis publicznego listingu: tylko skonfigurowane sloty; {@code version} unieważnia cache obrazka. */
public record PublicSiteSlotDto(String slot, long version) {
}
