package pl.photodrive.core.presentation.dto.user;

import jakarta.validation.constraints.NotBlank;

public record PasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {
}
