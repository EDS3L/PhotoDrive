package pl.photodrive.core.presentation.dto.user;

import jakarta.validation.constraints.NotBlank;

public record RemindPasswordRequest(@NotBlank String email, @NotBlank String token, @NotBlank String newPassword) {
}
