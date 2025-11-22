package pl.photodrive.core.application.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pl.photodrive.core.application.port.file.FileStoragePort;
import pl.photodrive.core.application.port.mail.MailSenderPort;
import pl.photodrive.core.domain.event.user.UserCreated;
import pl.photodrive.core.domain.model.Role;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventHandler {
    private final FileStoragePort fileStoragePort;
    private final MailSenderPort mailSenderPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePhotographCreated(UserCreated userCreated) {
        log.info("User created: {}", userCreated);
        String safeEmail = mailSenderPort.escapeHtml(userCreated.email());
        String safePassword = mailSenderPort.escapeHtml(userCreated.password());

        String accountCreatedTemplate = mailSenderPort.loadResourceAsString(
                        "templates/email/account-created-credentials.html").replace("{{email}}", safeEmail)
                .replace("{{password}}", safePassword);

        mailSenderPort.send(userCreated.email(),"Twoje konto zostało założone!", accountCreatedTemplate);

        if (userCreated.roles().contains(Role.PHOTOGRAPHER)) {
            fileStoragePort.createPhotographerFolder(userCreated.email());
        }
    }
}
