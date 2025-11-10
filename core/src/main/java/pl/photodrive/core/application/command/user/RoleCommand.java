package pl.photodrive.core.application.command.user;

import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.vo.UserId;

import java.util.UUID;

public record RoleCommand(UserId userId, Role role) {
}
