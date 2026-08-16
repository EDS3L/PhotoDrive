package pl.photodrive.core.application.command.auth;

public record RemindPasswordCommand(String email, String token, String newPassword) {
}
