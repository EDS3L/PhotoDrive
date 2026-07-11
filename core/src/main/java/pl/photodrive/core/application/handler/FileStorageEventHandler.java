package pl.photodrive.core.application.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pl.photodrive.core.application.event.FileStorageRequested;
import pl.photodrive.core.application.exception.StorageOperationException;
import pl.photodrive.core.application.port.file.FileStoragePort;
import pl.photodrive.core.application.port.file.TemporaryStoragePort;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileStorageEventHandler {

    private final FileStoragePort fileStoragePort;
    private final TemporaryStoragePort temporaryStoragePort;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleFileAddedToAlbum(FileStorageRequested event) {
        try {

            String tempId = event.tempId();

            if (!temporaryStoragePort.exists(tempId)) {
                log.error("[FileStorageEventHandler] Temporary file not found: {}", tempId);
                throw new StorageOperationException("Temporary file not found: " + tempId);
            }

            InputStream inputStream = temporaryStoragePort.getFile(tempId);

            fileStoragePort.saveFile(event.albumName(), event.fileName().value(), inputStream);

            temporaryStoragePort.delete(tempId);

        } catch (IOException e) {
            log.error("[FileStorageEventHandler] Failed to store file: {}", event.fileName(), e);
            throw new StorageOperationException("Failed to store file in local storage");
        }

    }

}
