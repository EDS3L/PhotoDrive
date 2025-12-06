package pl.photodrive.core.presentation.dto.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import pl.photodrive.core.domain.model.Role;

public record CreateUserRequest(@NotBlank String name, @Valid @NotBlank String email, @NotBlank String password,
                                Role role) {
}
