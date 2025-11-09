package pl.photodrive.core.application.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pl.photodrive.core.domain.event.AdminAlbumCreated;
import pl.photodrive.core.domain.event.PhotographCreateAlbum;
import pl.photodrive.core.domain.event.PhotographerRootAlbumCreated;
import pl.photodrive.core.domain.port.StoragePort;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageStructureCreator {

    private final StoragePort storagePort;

    @EventListener
    public void handle(PhotographerRootAlbumCreated event) {
        log.info("Received PhotographerRootAlbumCreated event for user {}", event.photographerId());
        storagePort.createFolderForPhotograph(event.photographerEmail().value());
    }

    @EventListener
    public void handleAdminCreatedFolder(AdminAlbumCreated event) {
        log.info("Received AdminFolderCreated event for user {}", event.admin().getId());
        storagePort.createAdminAlbumDir(event.albumName());
    }

    @EventListener
    public void handlePhotographCreatedFolder(PhotographCreateAlbum event) {
        log.info("Received PhotographCreateAlbum event for user {}", event.photograph().getId());
        storagePort.createClientAlbumDir(event.name(), event.photograph().getEmail().value());

    }
}
