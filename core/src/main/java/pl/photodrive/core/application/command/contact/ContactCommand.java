package pl.photodrive.core.application.command.contact;

/** Intencja: wyślij wiadomość z formularza kontaktowego. Budowana z {@code ContactRequest} przez kontroler. */
public record ContactCommand(
        String name,
        String email,
        String phone,
        String sessionType,
        String message
) {
}
