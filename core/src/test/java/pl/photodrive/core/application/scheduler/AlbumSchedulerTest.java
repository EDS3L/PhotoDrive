package pl.photodrive.core.application.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.photodrive.core.application.service.AlbumManagementService;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AlbumSchedulerTest {

    @Mock
    private AlbumManagementService albumManagementService;

    @InjectMocks
    private AlbumScheduler scheduler;

    @Test
    @DisplayName("The nightly cron delegates expired album removal to the service")
    void shouldDelegateExpiredAlbumRemoval() {
        // When
        scheduler.scheduledRemoveExpiredAlbums();

        // Then
        then(albumManagementService).should().removeExpiredAlbum();
    }
}
