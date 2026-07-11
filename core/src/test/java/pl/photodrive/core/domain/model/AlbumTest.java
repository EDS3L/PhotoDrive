package pl.photodrive.core.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.photodrive.core.domain.event.album.FileAddedResult;
import pl.photodrive.core.domain.event.album.FileAddedToAlbum;
import pl.photodrive.core.domain.event.album.FileVisibleStatusChanged;
import pl.photodrive.core.domain.exception.AlbumException;
import pl.photodrive.core.domain.exception.FileException;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.domain.vo.FileName;
import pl.photodrive.core.domain.vo.HashedPassword;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AlbumTest {

    private User admin;
    private User photographer;
    private User client;


    private final HashedPassword dummyPassword = new HashedPassword("Test1234!");

    @BeforeEach
    void setUp() {
        admin = User.create("Admin", new Email("admin@photodrive.pl"), dummyPassword, Role.ADMIN);
        photographer = User.create("Photographer", new Email("photographer@photodrive.pl"), dummyPassword, Role.PHOTOGRAPHER);
        client = User.create("Client", new Email("client@photodrive.pl"), dummyPassword, Role.CLIENT);
    }

    @Test
    void shouldCreateAlbumForAdmin() {
        Album album = Album.createForAdmin("System Album", admin);

        assertNotNull(album);
        assertEquals("System Album", album.getName());
        assertEquals(admin.getId().value(), album.getPhotographId());
    }

    @Test
    void shouldThrowExceptionWhenPhotographerTriesToCreateAdminAlbum() {
        assertThrows(AlbumException.class, () -> Album.createForAdmin("Fake Admin", photographer));
    }

    @Test
    void shouldThrowExceptionWhenAddingFilesExceedsStorage() {
        // Given
        Album album = Album.createForAdmin("StorageTest", admin);
        File file = File.create(new FileName("foto.jpg"), 2L * 1024 * 1024 * 1024, "image/jpeg");
        List<File> filesToAdd = List.of(file);

        // When & Then
        assertThrows(AlbumException.class, () -> album.addFiles(filesToAdd, 1, 0));
    }

    @Test
    void shouldRegisterEventWhenFileIsAdded() {
        // Given
        Album album = Album.createForAdmin("EventTest", admin);
        File file = File.create(new FileName("img.png"), 100L, "image/png");

        // When
        List<FileAddedResult> results = album.addFiles(List.of(file), 10, 0);

        // Then
        assertTrue(results.stream()
                .anyMatch(r -> r.event() instanceof FileAddedToAlbum));
    }

    @Test
    void shouldGrantAccessToAdminAlways() {
        // Given
        Album album = Album.createForClient("Private", photographer, client);

        // When & Then — admin powinien mieć dostęp nawet gdy visible = false
        assertTrue(album.hasAccessToGetFilesFromAlbum(admin, false));
    }

    @Test
    void shouldDenyAccessToClientWhenAlbumIsNotVisible() {
        // Given
        Album album = Album.createForClient("Secret", photographer, client);

        // When & Then
        assertThrows(AlbumException.class, () -> album.hasAccessToGetFilesFromAlbum(client, false));
    }

    @Test
    void shouldThrowExceptionWhenWatermarkingUnsupportedExtension() {
        // Given
        Album album = Album.createForClient("WatermarkTest", photographer, client);

        File videoFile = File.create(new FileName("video.mp4"), 100L, "video/mp4");
        FileId fileId = videoFile.getFileId();

        Map<FileId, File> photos = new HashMap<>();
        photos.put(fileId, videoFile);
        album.assignPhotosToAlbum(photos);

        // When & Then
        assertThrows(FileException.class, () ->
                album.changeWatermarkStatus(photographer, true, List.of(fileId), true)
        );
    }

    @Test
    void shouldSetWatermarkSuccessfully() {
        // Given — happy path dla watermark
        Album album = Album.createForClient("WatermarkHappy", photographer, client);
        File photo = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        FileId fileId = photo.getFileId();

        Map<FileId, File> photos = new HashMap<>();
        photos.put(fileId, photo);
        album.assignPhotosToAlbum(photos);

        // When
        album.changeWatermarkStatus(photographer, true, List.of(fileId), true);

        // Then
        assertTrue(album.getPhotos().get(fileId).isHasWatermark());
    }

    @Test
    void shouldThrowWhenEnablingWatermarkWithoutConfiguredPlatformWatermark() {
        // Given — admin nie wgrał loga → włączenie watermarku musi być zablokowane (guard serwerowy)
        Album album = Album.createForClient("NoLogo", photographer, client);
        File photo = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        FileId fileId = photo.getFileId();

        Map<FileId, File> photos = new HashMap<>();
        photos.put(fileId, photo);
        album.assignPhotosToAlbum(photos);

        // When & Then
        assertThrows(AlbumException.class, () ->
                album.changeWatermarkStatus(photographer, true, List.of(fileId), false));
        assertFalse(album.getPhotos().get(fileId).isHasWatermark());
    }

    @Test
    void shouldAllowDisablingWatermarkEvenWhenPlatformWatermarkNotConfigured() {
        // Given — plik z flagą; zdejmowanie watermarku nie może zależeć od obecności loga
        Album album = Album.createForClient("DisableAlways", photographer, client);
        File photo = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        FileId fileId = photo.getFileId();

        Map<FileId, File> photos = new HashMap<>();
        photos.put(fileId, photo);
        album.assignPhotosToAlbum(photos);
        album.changeWatermarkStatus(photographer, true, List.of(fileId), true);

        // When
        album.changeWatermarkStatus(photographer, false, List.of(fileId), false);

        // Then
        assertFalse(album.getPhotos().get(fileId).isHasWatermark());
    }

    @Test
    void shouldBeIdempotentWhenSomeFilesAlreadyWatermarked() {
        // Given — jeden plik już owatermarkowany, drugi nie
        Album album = Album.createForClient("MixedWatermark", photographer, client);
        File already = File.create(new FileName("a.jpg"), 100L, "image/jpeg");
        File fresh = File.create(new FileName("b.jpg"), 100L, "image/jpeg");
        FileId alreadyId = already.getFileId();
        FileId freshId = fresh.getFileId();

        Map<FileId, File> photos = new HashMap<>();
        photos.put(alreadyId, already);
        photos.put(freshId, fresh);
        album.assignPhotosToAlbum(photos);
        album.changeWatermarkStatus(photographer, true, List.of(alreadyId), true);

        // When — paczka z już-owatermarkowanym plikiem NIE może rzucać
        album.changeWatermarkStatus(photographer, true, List.of(alreadyId, freshId), true);

        // Then — oba mają watermark (toggle to czysty flip flagi — wersje watermarkowane
        // są komponowane przy serwowaniu, więc nie ma już zdarzeń plikowych do liczenia)
        assertTrue(album.getPhotos().get(alreadyId).isHasWatermark());
        assertTrue(album.getPhotos().get(freshId).isHasWatermark());
    }

    @Test
    void shouldCalcToGBCorrectly() {
        // Given
        Album album = Album.createForAdmin("Math", admin);

        // When
        long expected = 1073741824L; // 1024 * 1024 * 1024

        // Then
        assertEquals(expected, album.calcToGB(1));
    }

    @Test
    void shouldThrowExceptionWhenAdminTriesToSetTTDForOwnAlbum() {
        // Given
        Album adminAlbum = Album.createForAdmin("SystemAlbum", admin);
        Instant futureDate = Instant.now().plusSeconds(360);

        // When & Then
        assertThrows(AlbumException.class, () -> adminAlbum.setTTD(futureDate, admin, admin.getEmail().value()));
    }

    @Test
    void shouldThrowExceptionWhenSettingTTDInThePast() {
        // Given
        Album album = Album.createForClient("ClientAlbum", photographer, client);
        Instant pastDate = Instant.now().minusSeconds(360);

        // When & Then
        assertThrows(AlbumException.class, () -> album.setTTD(pastDate, photographer, photographer.getEmail().value()));
    }

    @Test
    void shouldSetTTDSuccessfully() {
        // Given — happy path dla TTD
        Album album = Album.createForClient("ClientAlbumTTD", photographer, client);
        Instant futureDate = Instant.now().plusSeconds(3600);

        // When
        album.setTTD(futureDate, photographer, photographer.getEmail().value());

        // Then
        assertEquals(futureDate, album.getTtd());
    }

    @Test
    void shouldClearPhotosWhenRemovingAlbum() {
        // Given
        Album album = Album.createForClient("ToDelete", photographer, client);
        File file = File.create(new FileName("pic.jpg"), 100L, "image/jpeg");
        album.addFile(file);

        assertFalse(album.getPhotos().isEmpty());

        // When
        album.removeFolder(album, photographer, photographer.getEmail().value());

        // Then
        assertTrue(album.getPhotos().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenClientTriesToRemoveAlbum() {
        // Given
        Album album = Album.createForClient("ClientTryDelete", photographer, client);

        // When & Then
        assertThrows(AlbumException.class, () ->
                album.removeFolder(album, client, photographer.getEmail().value())
        );
    }

    @Test
    void shouldThrowExceptionWhenRemovingNonExpiredAlbum() {
        // Given
        Album album = Album.createForClient("ValidAlbum", photographer, client);
        Instant futureDate = Instant.now().plusSeconds(3600);
        album.setTTD(futureDate, photographer, photographer.getEmail().value());

        // When & Then
        assertThrows(AlbumException.class, () -> album.removeExpiredAlbum());
    }

    // -----------------------------------------------------------------------
    // removeFiles
    // -----------------------------------------------------------------------

    @Test
    void shouldRemoveFileFromAlbumSuccessfully() {
        // Given
        Album album = Album.createForClient("RemoveTest", photographer, client);
        File file = File.create(new FileName("remove.jpg"), 100L, "image/jpeg");
        FileId fileId = file.getFileId();
        album.addFile(file);

        // When
        album.removeFiles(fileId, photographer);

        // Then
        assertFalse(album.getPhotos().containsKey(fileId));
    }

    @Test
    void shouldThrowWhenRemovingNonExistentFile() {
        // Given
        Album album = Album.createForClient("RemoveTest2", photographer, client);
        FileId nonExistent = new FileId(java.util.UUID.randomUUID());

        // When & Then
        assertThrows(AlbumException.class, () -> album.removeFiles(nonExistent, photographer));
    }

    @Test
    void shouldDenyClientRemovingFileFromOwnAlbum() {
        // Given
        Album album = Album.createForClient("RemoveTest3", photographer, client);
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        FileId fileId = file.getFileId();
        album.addFile(file);

        // When / Then
        assertThrows(AlbumException.class, () -> album.removeFiles(fileId, client));
        assertTrue(album.getPhotos().containsKey(fileId));
    }

    // -----------------------------------------------------------------------
    // renameFile
    // -----------------------------------------------------------------------

    @Test
    void shouldRenameFileInAlbumSuccessfully() {
        // Given
        Album album = Album.createForClient("RenameTest", photographer, client);
        File file = File.create(new FileName("old.jpg"), 100L, "image/jpeg");
        album.addFile(file);
        FileName newName = new FileName("new.jpg");

        // When
        album.renameFile(file.getFileId(), newName, photographer);

        // Then
        assertEquals(newName, album.getPhotos().get(file.getFileId()).getFileName());
    }

    @Test
    void shouldThrowWhenRenamingToAlreadyExistingFileName() {
        // Given
        Album album = Album.createForClient("RenameConflict", photographer, client);
        File file1 = File.create(new FileName("first.jpg"), 100L, "image/jpeg");
        File file2 = File.create(new FileName("second.jpg"), 100L, "image/jpeg");
        album.addFile(file1);
        album.addFile(file2);

        // When & Then
        assertThrows(AlbumException.class,
                () -> album.renameFile(file1.getFileId(), new FileName("second.jpg"), photographer));
    }

    @Test
    void shouldDenyClientRenamingFileInOwnAlbum() {
        // Given
        Album album = Album.createForClient("RenameDeny", photographer, client);
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        album.addFile(file);

        // When / Then
        assertThrows(AlbumException.class, () -> album.renameFile(file.getFileId(), new FileName("new.jpg"), client));
        assertEquals(new FileName("photo.jpg"), album.getPhotos().get(file.getFileId()).getFileName());
    }

    // -----------------------------------------------------------------------
    // changeFileVisibleStatus
    // -----------------------------------------------------------------------

    @Test
    void shouldMakeFileVisibleSuccessfully() {
        // Given
        Album album = Album.createForClient("VisibleTest", photographer, client);
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        FileId fileId = file.getFileId();
        album.addFile(file);

        // When
        album.changeFileVisibleStatus(List.of(fileId), true, photographer, photographer.getEmail());

        // Then
        assertTrue(album.getPhotos().get(fileId).isVisible());
    }

    @Test
    void shouldDenyClientChangingVisibilityInOwnAlbum() {
        // Given
        Album album = Album.createForClient("VisibleDeny", photographer, client);
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        album.addFile(file);

        // When / Then
        assertThrows(AlbumException.class, () ->
                album.changeFileVisibleStatus(List.of(file.getFileId()), true, client, client.getEmail()));
        assertFalse(album.getPhotos().get(file.getFileId()).isVisible());
    }

    @Test
    void shouldBeIdempotentWhenSomeFilesAlreadyVisible() {
        // Given — mieszane zaznaczenie: jeden plik już widoczny, drugi jeszcze nie
        Album album = Album.createForClient("MixedVisible", photographer, client);
        File alreadyVisible = File.create(new FileName("a.jpg"), 100L, "image/jpeg");
        File notVisible = File.create(new FileName("b.jpg"), 100L, "image/jpeg");
        album.addFile(alreadyVisible);
        album.addFile(notVisible);
        album.changeFileVisibleStatus(List.of(alreadyVisible.getFileId()), true, photographer, photographer.getEmail());

        // When — ta sama akcja na paczce zawierającej już-widoczny plik NIE może rzucać
        album.changeFileVisibleStatus(
                List.of(alreadyVisible.getFileId(), notVisible.getFileId()), true, photographer, photographer.getEmail());

        // Then — oba widoczne
        assertTrue(album.getPhotos().get(alreadyVisible.getFileId()).isVisible());
        assertTrue(album.getPhotos().get(notVisible.getFileId()).isVisible());
    }

    @Test
    void shouldReportOnlyNewlyChangedCountInVisibilityEvent() {
        // Given — dwa pliki, jeden już widoczny
        Album album = Album.createForClient("VisibleEventCount", photographer, client);
        File a = File.create(new FileName("a.jpg"), 100L, "image/jpeg");
        File b = File.create(new FileName("b.jpg"), 100L, "image/jpeg");
        album.addFile(a);
        album.addFile(b);
        album.changeFileVisibleStatus(List.of(a.getFileId()), true, photographer, photographer.getEmail());
        album.pullDomainEvents(); // czyścimy zdarzenie z przygotowania

        // When — paczka [a (już widoczny), b (nowy)] → faktycznie zmienia się tylko b
        album.changeFileVisibleStatus(
                List.of(a.getFileId(), b.getFileId()), true, photographer, photographer.getEmail());

        // Then — zdarzenie raportuje 1 (nie 2), więc klient nie dostaje maila o "już widocznych"
        FileVisibleStatusChanged event = album.pullDomainEvents().stream()
                .filter(FileVisibleStatusChanged.class::isInstance)
                .map(FileVisibleStatusChanged.class::cast)
                .findFirst().orElseThrow();
        assertEquals(1, event.sizeList());
    }

    @Test
    void shouldNotEmitVisibilityEventWhenNothingChanged() {
        // Given — plik już widoczny
        Album album = Album.createForClient("NoChangeEvent", photographer, client);
        File a = File.create(new FileName("a.jpg"), 100L, "image/jpeg");
        album.addFile(a);
        album.changeFileVisibleStatus(List.of(a.getFileId()), true, photographer, photographer.getEmail());
        album.pullDomainEvents();

        // When — ponowne "ustaw widoczne" na już-widocznym
        album.changeFileVisibleStatus(List.of(a.getFileId()), true, photographer, photographer.getEmail());

        // Then — żadnego zdarzenia (brak spamu mailowego do klienta)
        assertTrue(album.pullDomainEvents().stream()
                .noneMatch(FileVisibleStatusChanged.class::isInstance));
    }

    // -----------------------------------------------------------------------
    // swapFiles
    // -----------------------------------------------------------------------

    @Test
    void shouldSwapFilesToTargetMapSuccessfully() {
        // Given
        Album source = Album.createForClient("Source", photographer, client);
        File file = File.create(new FileName("swap.jpg"), 100L, "image/jpeg");
        FileId fileId = file.getFileId();
        source.addFile(file);

        Album target = Album.createForClient("Target", photographer, client);

        // When
        List<File> removedFiles = source.swapFiles(photographer, target.getAlbumPath(), List.of(fileId));
        java.util.Map<FileId, File> incomingFiles = new java.util.LinkedHashMap<>();
        for (File f : removedFiles) {
            incomingFiles.put(f.getFileId(), f);
        }
        target.receiveFiles(incomingFiles);

        // Then
        assertFalse(source.getPhotos().containsKey(fileId));
        assertTrue(target.getPhotos().containsKey(fileId));
    }

    @Test
    void shouldThrowWhenClientTriesToSwapFiles() {
        // Given
        Album album = Album.createForClient("SwapDeny", photographer, client);
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        FileId fileId = file.getFileId();
        album.addFile(file);

        // When & Then
        assertThrows(AlbumException.class,
                () -> album.swapFiles(client, new pl.photodrive.core.domain.vo.AlbumPath("other/album"), List.of(fileId)));
    }

    @Test
    void shouldThrowWhenNonOwnerPhotographerTriesToSwap() {
        // Given — album należy do `photographer`, swap próbuje inny fotograf
        User otherPhotographer = User.create("Other", new Email("other@photodrive.pl"), dummyPassword, Role.PHOTOGRAPHER);
        Album album = Album.createForClient("OwnedSource", photographer, client);
        File file = File.create(new FileName("owned.jpg"), 100L, "image/jpeg");
        album.addFile(file);

        // When & Then — brak własności = odmowa (BOLA hardening)
        assertThrows(AlbumException.class,
                () -> album.swapFiles(otherPhotographer, new pl.photodrive.core.domain.vo.AlbumPath("other/album"), List.of(file.getFileId())));
    }

    @Test
    void shouldThrowWhenReceivingFileWithDuplicateNameInTarget() {
        // Given — album docelowy ma już plik o tej samej nazwie
        Album target = Album.createForClient("TargetDup", photographer, client);
        File existing = File.create(new FileName("dup.jpg"), 100L, "image/jpeg");
        target.addFile(existing);

        File incoming = File.create(new FileName("dup.jpg"), 100L, "image/jpeg");
        Map<FileId, File> incomingFiles = new HashMap<>();
        incomingFiles.put(incoming.getFileId(), incoming);

        // When & Then — kolizja nazw odrzucona zamiast cichego nadpisania
        assertThrows(AlbumException.class, () -> target.receiveFiles(incomingFiles));
    }

    // -----------------------------------------------------------------------
    // getFilePath
    // -----------------------------------------------------------------------

    @Test
    void shouldReturnJustAlbumNameForAdmin() {
        // Given
        Album album = Album.createForAdmin("AdminAlbum", admin);

        // When
        String path = album.getFilePath(admin, admin.getEmail().value());

        // Then
        assertEquals("AdminAlbum", path);
    }

    @Test
    void shouldReturnEmailPrefixedPathForPhotographer() {
        // Given
        Album album = Album.createForClient("ClientAlbum", photographer, client);
        String fullName = album.getName();

        // When
        String path = album.getFilePath(photographer, photographer.getEmail().value());

        // Then
        assertEquals(photographer.getEmail().value() + "/" + fullName, path);
    }

    @Test
    void shouldReturnPathForClientWithPhotographerEmailPrefix() {
        // Client is the owner, path includes photographer prefix
        // Given
        Album album = Album.createForClient("PathDeny", photographer, client);
        String fullName = album.getName();

        // When
        String path = album.getFilePath(client, photographer.getEmail().value());

        // Then
        assertEquals(photographer.getEmail().value() + "/" + fullName, path);
    }

    // -----------------------------------------------------------------------
    // canAccess
    // -----------------------------------------------------------------------

    @Test
    void shouldGrantAccessToAdminAlbum() {
        // Given
        Album album = Album.createForClient("CanAccessTest", photographer, client);

        // When / Then — admin always has access
        assertTrue(album.canAccess(admin.getId(), admin.getRoles()));
    }

    @Test
    void shouldGrantAccessToOwnerPhotographer() {
        // Given
        Album album = Album.createForClient("OwnerAccess", photographer, client);

        // When / Then
        assertTrue(album.canAccess(photographer.getId(), photographer.getRoles()));
    }

    @Test
    void shouldDenyAccessToOtherPhotographer() {
        // Given
        Album album = Album.createForClient("OtherPhotograph", photographer, client);
        User other = User.create("Other", new Email("other@photodrive.pl"), dummyPassword, Role.PHOTOGRAPHER);

        // When / Then
        assertFalse(album.canAccess(other.getId(), other.getRoles()));
    }

    @Test
    void shouldDenyAccessToClient() {
        // Given
        Album album = Album.createForClient("ClientNoAccess", photographer, client);

        // When / Then
        assertFalse(album.canAccess(client.getId(), client.getRoles()));
    }

    @Test
    void shouldAllowClientToReadOnlyVisibleFileInOwnAlbum() {
        // Given
        Album album = Album.createForClient("ClientVisibleRead", photographer, client);
        File visibleFile = File.create(new FileName("visible.jpg"), 100L, "image/jpeg");
        File hiddenFile = File.create(new FileName("hidden.jpg"), 100L, "image/jpeg");
        album.addFile(visibleFile);
        album.addFile(hiddenFile);
        album.changeFileVisibleStatus(List.of(visibleFile.getFileId()), true, photographer, photographer.getEmail());

        // When / Then
        assertTrue(album.canReadFile(client, "visible.jpg"));
        assertFalse(album.canReadFile(client, "hidden.jpg"));
        assertFalse(album.canReadFile(client, "missing.jpg"));
    }

    @Test
    void shouldExposeOnlyVisibleFilesAsPublicFiles() {
        // Given
        Album album = Album.createForClient("PublicVisibleRead", photographer, client);
        File visibleFile = File.create(new FileName("visible.jpg"), 100L, "image/jpeg");
        File hiddenFile = File.create(new FileName("hidden.jpg"), 100L, "image/jpeg");
        album.addFile(visibleFile);
        album.addFile(hiddenFile);
        album.changeFileVisibleStatus(List.of(visibleFile.getFileId()), true, photographer, photographer.getEmail());

        // When / Then
        assertTrue(album.hasVisibleFile("visible.jpg"));
        assertFalse(album.hasVisibleFile("hidden.jpg"));
        assertFalse(album.hasVisibleFile("missing.jpg"));
    }
}
