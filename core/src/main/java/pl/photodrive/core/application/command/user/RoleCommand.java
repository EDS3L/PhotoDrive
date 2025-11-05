package pl.photodrive.core.application.command.user;

import pl.photodrive.core.domain.model.Role;

import java.util.UUID;

public record RoleCommand(UUID id, Role role) {
}
