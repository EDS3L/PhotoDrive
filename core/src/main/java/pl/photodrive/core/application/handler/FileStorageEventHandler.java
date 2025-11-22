package pl.photodrive.core.application.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pl.photodrive.core.application.event.FileStorageRequested;
import pl.photodrive.core.application.port.file.TemporaryStoragePort;
import pl.photodrive.core.infrastructure.storage.LocalStorageAdapter;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileStorageEventHandler {

    private final LocalStorageAdapter localStorageAdapter;
    private final TemporaryStoragePort temporaryStoragePort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFileAddedToAlbum(FileStorageRequested event) {
        try {

            String tempId = event.tempId();

            if (!temporaryStoragePort.exists(tempId)) {
                log.error("[FileStorageEventHandler] Temporary file not found: {}", tempId);
                return;
            };

            InputStream inputStream = temporaryStoragePort.getFile(tempId);

            localStorageAdapter.saveFile(event.albumName(), event.fileName().value(), inputStream);

            temporaryStoragePort.delete(tempId);

        } catch (IOException e) {
            log.error("[FileStorageEventHandler] Failed to store file: {}", event.fileName(), e);
        }

    }
}
