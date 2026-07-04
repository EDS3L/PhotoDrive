package pl.photodrive.core.presentation.dto.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pl.photodrive.core.domain.model.Role;

public record CreateUserRequest(@NotBlank String name, @Valid @NotBlank String email, @NotNull Role role) {
}
