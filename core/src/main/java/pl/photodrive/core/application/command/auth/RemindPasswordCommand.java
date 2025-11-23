package pl.photodrive.core.application.command.auth;

import pl.photodrive.core.domain.vo.Email;

import java.util.UUID;

public record RemindPasswordCommand(Email email, UUID token, String newPassword) {
}
