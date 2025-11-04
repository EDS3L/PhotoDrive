package pl.photodrive.core.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(@NotBlank String name, @Valid  @NotBlank String email, @NotBlank String password) {
}
