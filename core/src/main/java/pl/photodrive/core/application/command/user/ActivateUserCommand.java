package pl.photodrive.core.application.command.user;

import pl.photodrive.core.domain.vo.UserId;

public record ActivateUserCommand(UserId userId, boolean active) {
}
