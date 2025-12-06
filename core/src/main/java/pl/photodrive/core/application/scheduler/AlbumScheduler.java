package pl.photodrive.core.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pl.photodrive.core.application.service.AlbumManagementService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumScheduler {

    private final AlbumManagementService albumManagementService;

    //    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    @Scheduled(cron = "0 0 0 * * *")
    public void scheduledRemoveExpiredAlbums() {
        log.info("Starting scheduled remove expired albums");

        albumManagementService.removeExpiredAlbum();
    }
}
