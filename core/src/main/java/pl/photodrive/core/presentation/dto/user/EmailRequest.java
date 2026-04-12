package pl.photodrive.core.presentation.dto.user;

import jakarta.validation.constraints.NotBlank;

public record EmailRequest(@NotBlank String newEmail) {
}
