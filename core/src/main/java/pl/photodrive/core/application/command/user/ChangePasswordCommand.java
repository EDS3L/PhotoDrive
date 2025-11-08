package pl.photodrive.core.application.command.user;

import pl.photodrive.core.domain.vo.UserId;

public record ChangePasswordCommand(UserId userId, String currentPassword, String newPassword) {
}
