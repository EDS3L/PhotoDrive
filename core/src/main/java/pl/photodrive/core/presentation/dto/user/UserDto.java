package pl.photodrive.core.presentation.dto.user;

import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.UserId;

import java.util.List;
import java.util.Set;

public record UserDto(UserId id, String name, Email email, Set<Role> roles, boolean isActive,
                      boolean changePasswordOnNextLogin, List<UserId> assignedUsers) {
}
