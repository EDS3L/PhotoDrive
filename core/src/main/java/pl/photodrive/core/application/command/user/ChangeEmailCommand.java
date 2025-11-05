package pl.photodrive.core.application.command.user;

import pl.photodrive.core.domain.vo.Email;

import java.util.UUID;

public record ChangeEmailCommand(UUID id, String newEmail) {
}
