package pl.photodrive.core.application.command.contact;

public record ContactCommand(
        String name,
        String email,
        String phone,
        String sessionType,
        String message
) {
}
