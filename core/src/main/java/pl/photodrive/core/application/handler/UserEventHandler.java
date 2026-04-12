package pl.photodrive.core.application.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pl.photodrive.core.application.event.UserCredentialsNotification;
import pl.photodrive.core.application.port.file.FileStoragePort;
import pl.photodrive.core.application.port.mail.MailSenderPort;
import pl.photodrive.core.domain.event.user.PasswordTokenCreated;
import pl.photodrive.core.domain.event.user.UserCreated;
import pl.photodrive.core.domain.event.user.UserRemindedPassword;
import pl.photodrive.core.domain.model.Role;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventHandler {
    private final FileStoragePort fileStoragePort;
    private final MailSenderPort mailSenderPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePhotographCreated(UserCreated userCreated) {
        log.info("User created with id: {}", userCreated.userId());

        if (userCreated.roles().contains(Role.PHOTOGRAPHER)) {
            fileStoragePort.createPhotographerFolder(userCreated.email());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCredentialsNotification(UserCredentialsNotification event) {
        try {
            String safeEmail = mailSenderPort.escapeHtml(event.email());
            String safePassword = mailSenderPort.escapeHtml(event.rawPassword());

            String accountCreatedTemplate = mailSenderPort.loadResourceAsString(
                    "templates/email/account-created-credentials.html")
                    .replace("{{email}}", safeEmail)
                    .replace("{{password}}", safePassword);

            mailSenderPort.send(event.email(), "Twoje konto zostało założone!", accountCreatedTemplate);
        } catch (Exception e) {
            log.error("Failed to send credentials notification to {}: {}", event.email(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTokenCreated(PasswordTokenCreated passwordTokenCreated) {
        try {
            log.info("User token created: {}", passwordTokenCreated);
            String safeToken = mailSenderPort.escapeHtml(String.valueOf(passwordTokenCreated.token()));

            String tokenCreatedTemplate = mailSenderPort.loadResourceAsString("templates/email/password_reset_token.html").replace(
                    "{{token}}",
                    safeToken);

            mailSenderPort.send(passwordTokenCreated.email(), "Zrestartuj swoje hasło", tokenCreatedTemplate);
        } catch (Exception e) {
            log.error("Failed to send password reset token to {}: {}", passwordTokenCreated.email(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRemindPassword(UserRemindedPassword userRemindedPassword) {
        try {
            log.info("User reminded password: {}", userRemindedPassword);

            String remindPasswordTemplate = mailSenderPort.loadResourceAsString("templates/email/password_changed.html");

            mailSenderPort.send(userRemindedPassword.email(), "Twoje hasło zostało zmienione", remindPasswordTemplate);
        } catch (Exception e) {
            log.error("Failed to send password change notification to {}: {}", userRemindedPassword.email(), e.getMessage(), e);
        }
    }
}
