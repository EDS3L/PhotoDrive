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
import static org.mockito.BDDMockito.*;

/**
 * Saving a file runs BEFORE_COMMIT: the bytes move from temporary storage to the target
 * disk, and only then is the metadata committed. Every failure MUST blow up,
 * otherwise the database would keep a row with no file behind it.
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
    @DisplayName("Upload moves the file from temporary storage to disk and cleans the temporary copy")
    void shouldMoveFileFromTempToStorage() throws IOException {
        // Given
        InputStream data = new ByteArrayInputStream("bytes".getBytes());
        given(temporaryStoragePort.exists("temp-123")).willReturn(true);
        given(temporaryStoragePort.getFile("temp-123")).willReturn(data);

        // When
        handler.handleFileAddedToAlbum(event());

        // Then
        then(fileStoragePort).should().saveFile("foto/sesja", "zdjecie.jpg", data);
        then(temporaryStoragePort).should().delete("temp-123");
    }

    @Test
    @DisplayName("Missing temporary file aborts the transaction, so no orphan row is committed")
    void shouldThrowWhenTemporaryFileMissing() throws IOException {
        // Given
        given(temporaryStoragePort.exists("temp-123")).willReturn(false);

        // When / Then
        assertThatThrownBy(() -> handler.handleFileAddedToAlbum(event()))
                .isInstanceOf(StorageOperationException.class)
                .hasMessageContaining("temp-123");

        then(fileStoragePort).should(never()).saveFile(anyString(), anyString(), any());
        then(temporaryStoragePort).should(never()).delete(anyString());
    }

    @Test
    @DisplayName("Failed disk write aborts the transaction and keeps the temporary file")
    void shouldThrowAndKeepTempWhenSaveFails() throws IOException {
        // Given
        given(temporaryStoragePort.exists("temp-123")).willReturn(true);
        given(temporaryStoragePort.getFile("temp-123")).willReturn(new ByteArrayInputStream(new byte[0]));
        willThrow(new IOException("disk full")).given(fileStoragePort).saveFile(anyString(), anyString(), any());

        // When / Then
        assertThatThrownBy(() -> handler.handleFileAddedToAlbum(event()))
                .isInstanceOf(StorageOperationException.class);

        then(temporaryStoragePort).should(never()).delete(anyString());
    }
}
