package pl.photodrive.core.application.command.user;

import pl.photodrive.core.domain.vo.UserId;

import java.util.UUID;

public record ActivateUserCommand(UUID userId, boolean active) {
}
