package pl.photodrive.core.application.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.photodrive.core.application.service.AlbumManagementService;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AlbumSchedulerTest {

    @Mock
    private AlbumManagementService albumManagementService;

    @InjectMocks
    private AlbumScheduler scheduler;

    @Test
    @DisplayName("Cron kasowania wygasłych albumów deleguje do serwisu")
    void shouldDelegateExpiredAlbumRemoval() {
        scheduler.scheduledRemoveExpiredAlbums();

        verify(albumManagementService).removeExpiredAlbum();
    }
}
