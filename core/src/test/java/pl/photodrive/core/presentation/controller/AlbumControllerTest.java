package pl.photodrive.core.presentation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import pl.photodrive.core.application.port.file.TemporaryStoragePort;
import pl.photodrive.core.application.service.AlbumManagementService;
import pl.photodrive.core.domain.vo.FileId;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Upload otwiera strumień części multipart, żeby przelać ją do magazynu tymczasowego.
 * Ten strumień jest zasobem systemowym — jeśli kontroler go nie zamknie, nikt tego
 * za niego nie zrobi (ani {@code saveTemporary}, ani sprzątanie części przez Springa).
 */
@ExtendWith(MockitoExtension.class)
class AlbumControllerTest {

    @Mock
    private AlbumManagementService albumService;

    @Mock
    private TemporaryStoragePort temporaryStorageService;

    @InjectMocks
    private AlbumController controller;

    @Test
    @DisplayName("Every multipart part is closed after being copied to temporary storage, so a bulk upload does not leak one descriptor per photo")
    void shouldCloseEveryMultipartStreamAfterUpload() throws IOException {
        // Given - two photos in one request, each handing out a descriptor we can watch
        ReflectionTestUtils.setField(controller, "maxTotalSizeBytes", 1_000_000L);
        TrackingMultipartFile first = new TrackingMultipartFile("jeden.jpg");
        TrackingMultipartFile second = new TrackingMultipartFile("dwa.jpg");
        given(temporaryStorageService.saveTemporary(any())).willReturn("temp-1", "temp-2");
        given(albumService.addFilesToAlbum(any()))
                .willReturn(List.of(new FileId(UUID.randomUUID()), new FileId(UUID.randomUUID())));

        // When
        controller.addFilesToClientAlbum(UUID.randomUUID(), List.of(first, second));

        // Then - not just the last one: a leak on any part accumulates across the session
        assertThat(first.stream.closed).isTrue();
        assertThat(second.stream.closed).isTrue();
    }

    /** Minimalna część multipart, która oddaje JEDEN śledzony strumień. */
    private static final class TrackingMultipartFile implements MultipartFile {

        private final String name;
        private final TrackingInputStream stream = new TrackingInputStream();

        private TrackingMultipartFile(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return "files";
        }

        @Override
        public String getOriginalFilename() {
            return name;
        }

        @Override
        public String getContentType() {
            return "image/jpeg";
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public long getSize() {
            return 5L;
        }

        @Override
        public byte[] getBytes() {
            return "bytes".getBytes();
        }

        @Override
        public InputStream getInputStream() {
            return stream;
        }

        @Override
        public void transferTo(java.io.File dest) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class TrackingInputStream extends ByteArrayInputStream {
        private boolean closed = false;

        private TrackingInputStream() {
            super("bytes".getBytes());
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
