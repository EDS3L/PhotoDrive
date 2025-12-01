package pl.photodrive.core.presentation.dto.user;

import java.util.List;
import java.util.UUID;

public record AssignUserRequest(List<UUID> userIdList) {
}
