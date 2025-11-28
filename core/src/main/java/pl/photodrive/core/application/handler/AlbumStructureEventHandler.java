package pl.photodrive.core.application.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pl.photodrive.core.application.exception.StorageOperationException;
import pl.photodrive.core.application.port.file.FileStoragePort;
import pl.photodrive.core.domain.event.album.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlbumStructureEventHandler {
    private final FileStoragePort fileStoragePort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAdminAlbumCreated(AdminAlbumCreated event) {
        log.info("Handling AdminAlbumCreated event for album: {}", event.albumName());

        try {
            fileStoragePort.createAdminAlbum(event.albumName());
            log.info("Successfully created admin album folder: {}", event.albumName());
        } catch (Exception e) {
            throw new StorageOperationException("Failed to create admin album");
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePhotographCreateAlbum(PhotographCreateAlbum event) {
        log.info("Handling PhotographCreateAlbum event for album: {} by photographer: {}",
                event.name(),
                event.photograph().getEmail().value());

        try {
            fileStoragePort.createClientAlbum(event.name(), event.photograph().getEmail().value());
            log.info("Successfully created client album folder: {}/{}",
                    event.photograph().getEmail().value(),
                    event.name());
        } catch (Exception e) {
            throw new StorageOperationException("Failed to create client album");
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePhotographDeleteAlbum(PhotographRemoveAlbum event) {
        log.info("Handling PhotographRemove event for album: {} from photographer: {}",
                event.albumName(),
                event.photographerEmail());

        try {
            fileStoragePort.deleteFolder(event.albumName(), event.photographerEmail());
            log.info("Successfully deleted folder");
        } catch (Exception e) {
            throw new StorageOperationException("Failed to remove folder");
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRenameFile(FileRenamedInAlbum event) {
        log.info("File renamed");

        try {
            fileStoragePort.renameFile(event.path(), event.oldFileName().value(),event.newFileName().value());
        } catch (Exception e) {
            throw new StorageOperationException("Failed to rename file");
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRemoveFile(FileRemovedFromAlbum event) {
        log.info("File removed");
        try {
            fileStoragePort.deleteFile(event.path(), event.fileName());
        } catch (Exception e) {
            throw new StorageOperationException("Failed to remove file");
        }
    }

}
