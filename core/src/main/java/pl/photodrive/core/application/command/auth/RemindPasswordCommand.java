package pl.photodrive.core.application.command.auth;

import java.util.UUID;

public record RemindPasswordCommand(String email, UUID token, String newPassword) {
}
