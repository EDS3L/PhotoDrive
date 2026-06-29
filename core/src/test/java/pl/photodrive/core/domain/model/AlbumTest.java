package pl.photodrive.core.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.photodrive.core.domain.event.album.FileAddedResult;
import pl.photodrive.core.domain.event.album.FileAddedToAlbum;
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
                album.changeWatermarkStatus(photographer, true, List.of(fileId))
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
        album.changeWatermarkStatus(photographer, true, List.of(fileId));

        // Then
        assertTrue(album.getPhotos().get(fileId).isHasWatermark());
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
