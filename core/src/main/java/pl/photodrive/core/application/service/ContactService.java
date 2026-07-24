package pl.photodrive.core.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.photodrive.core.application.command.contact.ContactCommand;
import pl.photodrive.core.application.port.mail.MailSenderPort;

/**
 * Obsługuje zapytania z publicznego formularza kontaktowego. Nie ma tu agregatu ani zapisu do bazy —
 * to bezstanowy przepływ „wejście → e-mail", więc idzie prosto przez {@link MailSenderPort}, a nie przez
 * zdarzenie domenowe (te są wiązane z fazą transakcji, której tu nie ma).
 *
 * <p>Wysyłane są dwa maile:
 * <ol>
 *   <li><b>Powiadomienie</b> do skrzynki studia (Reply-To = adres gościa, żeby „Odpowiedz" trafiało do niego).
 *       Jego porażka <b>przerywa</b> operację — gość ma wiedzieć, że wiadomość nie dotarła.</li>
 *   <li><b>Potwierdzenie</b> do gościa (Reply-To = studio). Jego porażka jest <b>tolerowana</b> (log) —
 *       skoro powiadomienie doszło, wiadomość jest dostarczona; brak potwierdzenia nie może wywalić żądania.</li>
 * </ol>
 *
 * <p>Wszystkie pola pochodzą od anonima, więc przed wstawieniem do HTML-a maila są escapowane
 * ({@link MailSenderPort#escapeHtml}); treść wiadomości dodatkowo zamienia znaki nowej linii na {@code <br>}
 * <b>po</b> escapowaniu, żeby zachować układ bez otwierania furtki na wstrzyknięcie.
 */
@Slf4j
@Service
public class ContactService {

    private static final String NOTIFICATION_TEMPLATE = "templates/email/contact-notification.html";
    private static final String CONFIRMATION_TEMPLATE = "templates/email/contact-confirmation.html";

    private final MailSenderPort mailSenderPort;
    private final String recipient;

    public ContactService(MailSenderPort mailSenderPort,
                          @Value("${app.contact.recipient}") String recipient) {
        this.mailSenderPort = mailSenderPort;
        this.recipient = recipient;
    }

    public void handle(ContactCommand command) {
        String safeName = mailSenderPort.escapeHtml(command.name());
        String safeEmail = mailSenderPort.escapeHtml(command.email());
        String safePhone = mailSenderPort.escapeHtml(blankToDash(command.phone()));
        String safeSessionType = mailSenderPort.escapeHtml(command.sessionType());
        String safeMessage = toHtmlParagraph(mailSenderPort.escapeHtml(command.message()));

        String notification = mailSenderPort.loadResourceAsString(NOTIFICATION_TEMPLATE)
                .replace("{{name}}", safeName)
                .replace("{{email}}", safeEmail)
                .replace("{{phone}}", safePhone)
                .replace("{{sessionType}}", safeSessionType)
                .replace("{{message}}", safeMessage);

        // Reply-To = adres gościa (raw, zwalidowany @Email w DTO), żeby odpowiedź studia szła wprost do niego.
        mailSenderPort.send(recipient,
                "Nowe zapytanie ze strony: " + command.sessionType(),
                notification,
                command.email());

        sendConfirmation(command, safeName, safeMessage);
    }

    /** Potwierdzenie dla gościa — best-effort: jego porażka nie może cofnąć dostarczonego już powiadomienia. */
    private void sendConfirmation(ContactCommand command, String safeName, String safeMessage) {
        try {
            String confirmation = mailSenderPort.loadResourceAsString(CONFIRMATION_TEMPLATE)
                    .replace("{{name}}", safeName)
                    .replace("{{message}}", safeMessage);

            mailSenderPort.send(command.email(),
                    "Dziękujemy za wiadomość — PhotoDrive",
                    confirmation,
                    recipient);
        } catch (Exception e) {
            log.warn("Failed to send contact confirmation to {}: {}", command.email(), e.getMessage());
        }
    }

    private String blankToDash(String value) {
        return (value == null || value.isBlank()) ? "—" : value;
    }

    private String toHtmlParagraph(String escaped) {
        return escaped.replace("\r\n", "\n").replace("\n", "<br>");
    }
}
