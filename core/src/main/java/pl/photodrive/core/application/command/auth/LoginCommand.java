package pl.photodrive.core.application.command.auth;

public record LoginCommand(String email, String rawPassword) {
}
