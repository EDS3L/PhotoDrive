package pl.photodrive.core.presentation.dto.album;

/** Prezentacja zakładki portfolio; limit długości etykiety egzekwuje domena ({@code Album}). */
public record DisplaySettingsRequest(String displayName, int displayOrder) {
}
