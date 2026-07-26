package pl.photodrive.core.presentation.dto.user;

import jakarta.validation.constraints.NotBlank;

/**
 * Kod autoryzacji jest tu {@code String}, a nie {@code UUID} — patrz
 * {@code RemindPasswordCommand}: literówka w przepisywanym kodzie ma dać ten sam
 * błąd 400 co kod nietrafiony, a nie 500 z deserializacji (B.14/B.45).
 */
public record RemindPasswordRequest(@NotBlank String email, @NotBlank String token, @NotBlank String newPassword) {
}
