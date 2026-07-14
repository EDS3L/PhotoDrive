package pl.photodrive.core.application.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.photodrive.core.application.exception.StorageOperationException;
import pl.photodrive.core.application.port.file.FileStoragePort;
import pl.photodrive.core.application.port.mail.MailSenderPort;
import pl.photodrive.core.domain.event.album.*;
import pl.photodrive.core.domain.vo.AlbumPath;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.FileName;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

/**
 * Event handlers are the file-to-database consistency mechanism: file operations run
 * BEFORE_COMMIT (so a failure rolls the transaction back), mails run AFTER_COMMIT.
 * These tests prove that a failed file operation really does blow up, instead of being swallowed.
 */
@ExtendWith(MockitoExtension.class)
class AlbumStructureEventHandlerTest {

    @Mock
    private FileStoragePort fileStoragePort;

    @Mock
    private MailSenderPort mailSenderPort;

    @InjectMocks
    private AlbumStructureEventHandler handler;

    @Test
    @DisplayName("Creating an admin album creates its storage folder")
    void shouldCreateAdminAlbumFolder() {
        // When
        handler.handleAdminAlbumCreated(new AdminAlbumCreated("portfolio-sluby", "admin@photodrive.dev"));

        // Then
        then(fileStoragePort).should().createAdminAlbum("portfolio-sluby");
    }

    @Test
    @DisplayName("Failure to create the admin album folder aborts the transaction")
    void shouldThrowWhenAdminAlbumFolderFails() {
        // Given
        willThrow(new RuntimeException("disk full")).given(fileStoragePort).createAdminAlbum(anyString());

        // When / Then
        assertThatThrownBy(() -> handler.handleAdminAlbumCreated(
                new AdminAlbumCreated("portfolio", "admin@photodrive.dev")))
                .isInstanceOf(StorageOperationException.class);
    }

    @Test
    @DisplayName("Creating a client album creates its folder under the photographer")
    void shouldCreateClientAlbumFolder() {
        // When
        handler.handlePhotographCreateAlbum(new PhotographCreateAlbum("foto@photodrive.dev", "sesja-anny"));

        // Then
        then(fileStoragePort).should().createClientAlbum("sesja-anny", "foto@photodrive.dev");
    }

    @Test
    @DisplayName("Failure to create the client album folder aborts the transaction")
    void shouldThrowWhenClientAlbumFolderFails() {
        // Given
        willThrow(new RuntimeException("boom")).given(fileStoragePort).createClientAlbum(anyString(), anyString());

        // When / Then
        assertThatThrownBy(() -> handler.handlePhotographCreateAlbum(
                new PhotographCreateAlbum("foto@photodrive.dev", "sesja")))
                .isInstanceOf(StorageOperationException.class);
    }

    @Test
    @DisplayName("Removing an album deletes its folder from storage")
    void shouldDeleteAlbumFolder() {
        // When
        handler.handlePhotographDeleteAlbum(new PhotographRemoveAlbum("foto@photodrive.dev/sesja"));

        // Then
        then(fileStoragePort).should().deleteFolder("foto@photodrive.dev/sesja");
    }

    @Test
    @DisplayName("Failure to delete the album folder is reported, not swallowed")
    void shouldThrowWhenFolderDeleteFails() {
        // Given
        willThrow(new RuntimeException("locked")).given(fileStoragePort).deleteFolder(anyString());

        // When / Then
        assertThatThrownBy(() -> handler.handlePhotographDeleteAlbum(new PhotographRemoveAlbum("path")))
                .isInstanceOf(StorageOperationException.class);
    }

    @Test
    @DisplayName("Renaming a file renames it on disk as well")
    void shouldRenameFile() {
        // When
        handler.handleRenameFile(new FileRenamedInAlbum(
                "foto/sesja",
                FileName.of("stara.jpg"),
                FileName.of("nowa.jpg")));

        // Then
        then(fileStoragePort).should().renameFile("foto/sesja", "stara.jpg", "nowa.jpg");
    }

    @Test
    @DisplayName("Failed rename aborts the transaction, so disk and database stay consistent")
    void shouldThrowWhenRenameFails() {
        // Given
        willThrow(new RuntimeException("io")).given(fileStoragePort).renameFile(anyString(), anyString(), anyString());

        // When / Then
        assertThatThrownBy(() -> handler.handleRenameFile(new FileRenamedInAlbum(
                "p", FileName.of("a.jpg"), FileName.of("b.jpg"))))
                .isInstanceOf(StorageOperationException.class);
    }

    @Test
    @DisplayName("Removing a file deletes it from disk")
    void shouldRemoveFile() {
        // When
        handler.handleRemoveFile(new FileRemovedFromAlbum("foto/sesja", "zdjecie.jpg"));

        // Then
        then(fileStoragePort).should().deleteFile("foto/sesja", "zdjecie.jpg");
    }

    @Test
    @DisplayName("Failure to delete the file is reported, not swallowed")
    void shouldThrowWhenFileDeleteFails() {
        // Given
        willThrow(new RuntimeException("io")).given(fileStoragePort).deleteFile(anyString(), anyString());

        // When / Then
        assertThatThrownBy(() -> handler.handleRemoveFile(new FileRemovedFromAlbum("p", "f.jpg")))
                .isInstanceOf(StorageOperationException.class);
    }

    @Test
    @DisplayName("Setting TTD mails the client with the date and time filled in")
    void shouldSendTtdMail() {
        // Given
        given(mailSenderPort.loadResourceAsString("templates/email/ttd-set.html")).willReturn("<p>{{date}} {{time}}</p>");

        // When
        handler.handleTtdSet(new TtdSet(Instant.now(), "klient@photodrive.dev"));

        // Then
        then(mailSenderPort).should().send(eq("klient@photodrive.dev"), anyString(), argThat(body ->
                !body.contains("{{date}}") && !body.contains("{{time}}")));
    }

    @Test
    @DisplayName("An expired album has its folder deleted")
    void shouldRemoveExpiredAlbumFolder() {
        // When
        handler.handleRemoveExpiredAlbum(new ExpiredAlbumRemoved(new AlbumPath("foto/sesja")));

        // Then
        then(fileStoragePort).should().deleteFolder("foto/sesja");
    }

    @Test
    @DisplayName("Revealing photos mails the client the number of files")
    void shouldSendVisibilityMailWithFileCount() {
        // Given
        given(mailSenderPort.loadResourceAsString("templates/email/files-visible-status.html")).willReturn("<p>{{fileCount}}</p>");

        // When
        handler.handleChangeFileVisibility(new FileVisibleStatusChanged(new Email("klient@photodrive.dev"), 7));

        // Then
        then(mailSenderPort).should().send(eq("klient@photodrive.dev"), anyString(), argThat(body ->
                body.contains("7") && !body.contains("{{fileCount}}")));
    }

    @Test
    @DisplayName("Swapping a file between albums moves it on disk")
    void shouldSwapFile() {
        // When
        handler.handleSwapFile(new FileSwaped(
                new AlbumPath("foto/zrodlo"),
                new AlbumPath("foto/cel"),
                FileName.of("zdjecie.jpg")));

        // Then
        then(fileStoragePort).should().swapFile("foto/zrodlo", "foto/cel", "zdjecie.jpg");
    }

    @Test
    @DisplayName("File operations send no mail, because they run before commit")
    void shouldNotSendMailFromFileOperations() {
        // Given
        handler.handleAdminAlbumCreated(new AdminAlbumCreated("a", "admin@photodrive.dev"));

        // When
        handler.handleRemoveFile(new FileRemovedFromAlbum("p", "f.jpg"));

        // Then
        then(mailSenderPort).shouldHaveNoInteractions();
    }
}
