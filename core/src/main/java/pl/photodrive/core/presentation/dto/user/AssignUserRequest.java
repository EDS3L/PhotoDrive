package pl.photodrive.core.presentation.dto.user;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AssignUserRequest(@NotNull @NotEmpty List<UUID> userIdList) {
}
