package pl.photodrive.core.application.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pl.photodrive.core.application.port.FileStoragePort;
import pl.photodrive.core.domain.event.user.UserCreated;
import pl.photodrive.core.domain.model.Role;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventHandler {
    private final FileStoragePort fileStoragePort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePhotographCreated(UserCreated userCreated) {
        log.info("User created: {}", userCreated);

        if(userCreated.roles().contains(Role.PHOTOGRAPHER)) {
            log.info("Photographer created: {}", userCreated);
            fileStoragePort.createPhotographerFolder(userCreated.email());
        }
    }
}
