package pl.photodrive.core.application.command.user;

import java.util.UUID;

public record ActivateUserCommand(UUID userId, boolean active) {
}
