package pl.photodrive.core.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.photodrive.core.application.command.contact.ContactCommand;
import pl.photodrive.core.application.port.mail.MailSenderPort;

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

        mailSenderPort.send(recipient,
                "Nowe zapytanie ze strony: " + command.sessionType(),
                notification,
                command.email());

        sendConfirmation(command, safeName, safeMessage);
    }

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
