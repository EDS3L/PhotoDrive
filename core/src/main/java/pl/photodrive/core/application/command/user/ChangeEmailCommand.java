package pl.photodrive.core.application.command.user;

import java.util.UUID;

public record ChangeEmailCommand(UUID userId, String newEmail) {
}
