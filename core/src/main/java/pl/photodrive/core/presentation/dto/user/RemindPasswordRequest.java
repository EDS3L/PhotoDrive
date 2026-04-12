package pl.photodrive.core.presentation.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RemindPasswordRequest(@NotBlank String email, @NotNull UUID token, @NotBlank String newPassword) {
}
