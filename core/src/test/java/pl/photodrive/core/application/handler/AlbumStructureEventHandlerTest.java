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

/**
 * Handlery zdarzeń to mechanizm spójności plik↔baza: operacje plikowe biegną
 * BEFORE_COMMIT (żeby porażka wycofała transakcję), a maile AFTER_COMMIT.
 * Testujemy, że porażka operacji plikowej NAPRAWDĘ wybucha (a nie jest łykana).
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
    @DisplayName("Utworzenie albumu admina zakłada folder w storage")
    void shouldCreateAdminAlbumFolder() {
        handler.handleAdminAlbumCreated(new AdminAlbumCreated("portfolio-sluby", "admin@photodrive.dev"));

        verify(fileStoragePort).createAdminAlbum("portfolio-sluby");
    }

    @Test
    @DisplayName("Porażka storage przy albumie admina przerywa transakcję")
    void shouldThrowWhenAdminAlbumFolderFails() {
        doThrow(new RuntimeException("disk full")).when(fileStoragePort).createAdminAlbum(anyString());

        assertThatThrownBy(() -> handler.handleAdminAlbumCreated(
                new AdminAlbumCreated("portfolio", "admin@photodrive.dev")))
                .isInstanceOf(StorageOperationException.class);
    }

    @Test
    @DisplayName("Utworzenie albumu klienta zakłada folder pod fotografem")
    void shouldCreateClientAlbumFolder() {
        handler.handlePhotographCreateAlbum(new PhotographCreateAlbum("foto@photodrive.dev", "sesja-anny"));

        verify(fileStoragePort).createClientAlbum("sesja-anny", "foto@photodrive.dev");
    }

    @Test
    @DisplayName("Porażka storage przy albumie klienta przerywa transakcję")
    void shouldThrowWhenClientAlbumFolderFails() {
        doThrow(new RuntimeException("boom")).when(fileStoragePort).createClientAlbum(anyString(), anyString());

        assertThatThrownBy(() -> handler.handlePhotographCreateAlbum(
                new PhotographCreateAlbum("foto@photodrive.dev", "sesja")))
                .isInstanceOf(StorageOperationException.class);
    }

    @Test
    @DisplayName("Usunięcie albumu kasuje folder")
    void shouldDeleteAlbumFolder() {
        handler.handlePhotographDeleteAlbum(new PhotographRemoveAlbum("foto@photodrive.dev/sesja"));

        verify(fileStoragePort).deleteFolder("foto@photodrive.dev/sesja");
    }

    @Test
    @DisplayName("Porażka kasowania folderu jest zgłaszana")
    void shouldThrowWhenFolderDeleteFails() {
        doThrow(new RuntimeException("locked")).when(fileStoragePort).deleteFolder(anyString());

        assertThatThrownBy(() -> handler.handlePhotographDeleteAlbum(new PhotographRemoveAlbum("path")))
                .isInstanceOf(StorageOperationException.class);
    }

    @Test
    @DisplayName("Zmiana nazwy pliku przenosi plik w storage")
    void shouldRenameFile() {
        handler.handleRenameFile(new FileRenamedInAlbum(
                "foto/sesja",
                FileName.of("stara.jpg"),
                FileName.of("nowa.jpg")));

        verify(fileStoragePort).renameFile("foto/sesja", "stara.jpg", "nowa.jpg");
    }

    @Test
    @DisplayName("Porażka zmiany nazwy przerywa transakcję (plik i baza nie rozjadą się)")
    void shouldThrowWhenRenameFails() {
        doThrow(new RuntimeException("io")).when(fileStoragePort).renameFile(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> handler.handleRenameFile(new FileRenamedInAlbum(
                "p", FileName.of("a.jpg"), FileName.of("b.jpg"))))
                .isInstanceOf(StorageOperationException.class);
    }

    @Test
    @DisplayName("Usunięcie pliku kasuje go ze storage")
    void shouldRemoveFile() {
        handler.handleRemoveFile(new FileRemovedFromAlbum("foto/sesja", "zdjecie.jpg"));

        verify(fileStoragePort).deleteFile("foto/sesja", "zdjecie.jpg");
    }

    @Test
    @DisplayName("Porażka usuwania pliku jest zgłaszana")
    void shouldThrowWhenFileDeleteFails() {
        doThrow(new RuntimeException("io")).when(fileStoragePort).deleteFile(anyString(), anyString());

        assertThatThrownBy(() -> handler.handleRemoveFile(new FileRemovedFromAlbum("p", "f.jpg")))
                .isInstanceOf(StorageOperationException.class);
    }

    @Test
    @DisplayName("Ustawienie TTD wysyła maila z datą i godziną")
    void shouldSendTtdMail() {
        when(mailSenderPort.loadResourceAsString("templates/email/ttd-set.html"))
                .thenReturn("<p>{{date}} {{time}}</p>");

        handler.handleTtdSet(new TtdSet(Instant.now(), "klient@photodrive.dev"));

        verify(mailSenderPort).send(eq("klient@photodrive.dev"), anyString(), argThat(body ->
                !body.contains("{{date}}") && !body.contains("{{time}}")));
    }

    @Test
    @DisplayName("Wygasły album kasuje swój folder")
    void shouldRemoveExpiredAlbumFolder() {
        handler.handleRemoveExpiredAlbum(new ExpiredAlbumRemoved(new AlbumPath("foto/sesja")));

        verify(fileStoragePort).deleteFolder("foto/sesja");
    }

    @Test
    @DisplayName("Zmiana widoczności wysyła maila z liczbą plików")
    void shouldSendVisibilityMailWithFileCount() {
        when(mailSenderPort.loadResourceAsString("templates/email/files-visible-status.html"))
                .thenReturn("<p>{{fileCount}}</p>");

        handler.handleChangeFileVisibility(new FileVisibleStatusChanged(new Email("klient@photodrive.dev"), 7));

        verify(mailSenderPort).send(eq("klient@photodrive.dev"), anyString(), argThat(body ->
                body.contains("7") && !body.contains("{{fileCount}}")));
    }

    @Test
    @DisplayName("Przeniesienie pliku między albumami rusza plik w storage")
    void shouldSwapFile() {
        handler.handleSwapFile(new FileSwaped(
                new AlbumPath("foto/zrodlo"),
                new AlbumPath("foto/cel"),
                FileName.of("zdjecie.jpg")));

        verify(fileStoragePort).swapFile("foto/zrodlo", "foto/cel", "zdjecie.jpg");
    }

    @Test
    @DisplayName("Operacje plikowe nie wysyłają maili (rozdział faz transakcji)")
    void fileOperationsShouldNotSendMail() {
        handler.handleAdminAlbumCreated(new AdminAlbumCreated("a", "admin@photodrive.dev"));
        handler.handleRemoveFile(new FileRemovedFromAlbum("p", "f.jpg"));

        verifyNoInteractions(mailSenderPort);
    }
}
