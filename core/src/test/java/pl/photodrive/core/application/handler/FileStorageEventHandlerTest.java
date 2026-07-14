package pl.photodrive.core.application.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.photodrive.core.application.event.FileStorageRequested;
import pl.photodrive.core.application.exception.StorageOperationException;
import pl.photodrive.core.application.port.file.FileStoragePort;
import pl.photodrive.core.application.port.file.TemporaryStoragePort;
import pl.photodrive.core.domain.vo.FileName;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Zapis pliku biegnie BEFORE_COMMIT: plik jedzie z magazynu tymczasowego na dysk
 * docelowy, a dopiero potem commituje się metadana w bazie. Każda porażka MUSI
 * wybuchnąć, inaczej w bazie zostałby rekord bez pliku.
 */
@ExtendWith(MockitoExtension.class)
class FileStorageEventHandlerTest {

    @Mock
    private FileStoragePort fileStoragePort;

    @Mock
    private TemporaryStoragePort temporaryStoragePort;

    @InjectMocks
    private FileStorageEventHandler handler;

    private static FileStorageRequested event() {
        return new FileStorageRequested("foto/sesja", FileName.of("zdjecie.jpg"), "temp-123");
    }

    @Test
    @DisplayName("Plik jedzie z magazynu tymczasowego na dysk, a temp jest sprzątany")
    void shouldMoveFileFromTempToStorage() throws IOException {
        InputStream data = new ByteArrayInputStream("bytes".getBytes());
        when(temporaryStoragePort.exists("temp-123")).thenReturn(true);
        when(temporaryStoragePort.getFile("temp-123")).thenReturn(data);

        handler.handleFileAddedToAlbum(event());

        verify(fileStoragePort).saveFile("foto/sesja", "zdjecie.jpg", data);
        verify(temporaryStoragePort).delete("temp-123");
    }

    @Test
    @DisplayName("Brak pliku tymczasowego przerywa transakcję (brak sieroty w bazie)")
    void shouldThrowWhenTemporaryFileMissing() throws IOException {
        when(temporaryStoragePort.exists("temp-123")).thenReturn(false);

        assertThatThrownBy(() -> handler.handleFileAddedToAlbum(event()))
                .isInstanceOf(StorageOperationException.class)
                .hasMessageContaining("temp-123");

        verify(fileStoragePort, never()).saveFile(anyString(), anyString(), any());
        verify(temporaryStoragePort, never()).delete(anyString());
    }

    @Test
    @DisplayName("Błąd zapisu na dysk przerywa transakcję, a temp NIE jest kasowany")
    void shouldThrowAndKeepTempWhenSaveFails() throws IOException {
        when(temporaryStoragePort.exists("temp-123")).thenReturn(true);
        when(temporaryStoragePort.getFile("temp-123")).thenReturn(new ByteArrayInputStream(new byte[0]));
        doThrow(new IOException("disk full")).when(fileStoragePort).saveFile(anyString(), anyString(), any());

        assertThatThrownBy(() -> handler.handleFileAddedToAlbum(event()))
                .isInstanceOf(StorageOperationException.class);

        verify(temporaryStoragePort, never()).delete(anyString());
    }
}
