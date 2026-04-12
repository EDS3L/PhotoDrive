package pl.photodrive.core.application.event;

public record UserCredentialsNotification(String email, String rawPassword) {
}
