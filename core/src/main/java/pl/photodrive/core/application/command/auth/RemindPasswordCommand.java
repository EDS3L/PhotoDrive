package pl.photodrive.core.application.command.auth;

import pl.photodrive.core.domain.vo.Email;

import java.util.UUID;

public record RemindPasswordCommand(String email, UUID token, String newPassword) {
}
