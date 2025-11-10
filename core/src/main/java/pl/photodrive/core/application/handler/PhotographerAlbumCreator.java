package pl.photodrive.core.application.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pl.photodrive.core.application.command.album.CreateAlbumForPhotographer;
import pl.photodrive.core.application.service.AlbumManagementService;
import pl.photodrive.core.domain.event.user.UserCreated;
import pl.photodrive.core.domain.model.Role;

@Slf4j
@Component
@RequiredArgsConstructor
public class PhotographerAlbumCreator {

    private final AlbumManagementService albumManagementService;

    @EventListener
    public void handlerUserCreatedEvent(UserCreated event) {
        log.info("Otrzymanio zdarzenie UserCreated dla użytkownika: {}", event.userId());

        if(event.roles().contains(Role.PHOTOGRAPHER)) {
            log.info("Użytkownik {} jest FOTOGRAFEM. Tworzę dla niego główny album.", event.userId());
            albumManagementService.createAlbumForPhotographer(new CreateAlbumForPhotographer(event.email(),event.userId()));
        }
    }
}
