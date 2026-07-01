package pl.photodrive.core.presentation.dto.user;

import pl.photodrive.core.domain.model.Role;

import java.util.List;
import java.util.Set;

public record UserDto(String id, String name, String email, Set<Role> roles, boolean isActive,
                      boolean changePasswordOnNextLogin, List<String> assignedUsers) {
}
