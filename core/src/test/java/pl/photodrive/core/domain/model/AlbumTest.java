package pl.photodrive.core.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.photodrive.core.domain.event.album.FileAddedResult;
import pl.photodrive.core.domain.event.album.FileAddedToAlbum;
import pl.photodrive.core.domain.event.album.FileVisibleStatusChanged;
import pl.photodrive.core.domain.exception.AlbumException;
import pl.photodrive.core.domain.exception.DomainSecurityException;
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
    @DisplayName("Admin album is created with the admin as both photographer and client")
    void shouldCreateAlbumForAdmin() {
        // When
        Album album = Album.createForAdmin("System Album", admin);

        // Then
        assertNotNull(album);
        assertEquals("System Album", album.getName());
        assertEquals(admin.getId().value(), album.getPhotographId());
    }

    @Test
    @DisplayName("Only an admin can create an admin (portfolio) album")
    void shouldThrowExceptionWhenPhotographerTriesToCreateAdminAlbum() {
        // When / Then
        assertThrows(DomainSecurityException.class, () -> Album.createForAdmin("Fake Admin", photographer));
    }

    @Test
    @DisplayName("Upload is rejected when it would exceed the storage quota")
    void shouldThrowExceptionWhenAddingFilesExceedsStorage() {
        // Given
        Album album = Album.createForAdmin("StorageTest", admin);
        File file = File.create(new FileName("foto.jpg"), 2L * 1024 * 1024 * 1024, "image/jpeg");
        List<File> filesToAdd = List.of(file);

        // When & Then
        assertThrows(AlbumException.class, () -> album.addFiles(filesToAdd, 1, 0));
    }

    @Test
    @DisplayName("Adding a file registers a domain event so the bytes are written to disk before commit")
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
    @DisplayName("A photo uploaded to an admin (portfolio) album is visible at once, so it reaches the site without a manual reveal")
    void shouldMakeUploadedFileVisibleInAdminAlbum() {
        // Given - an admin album is the portfolio: no client to hide raw shots from
        Album album = Album.createForAdmin("Portfolio", admin);
        File file = File.create(new FileName("okladka.jpg"), 100L, "image/jpeg");

        // When
        album.addFile(file);

        // Then
        assertTrue(album.getPhotos().get(file.getFileId()).isVisible());
    }

    @Test
    @DisplayName("A photo uploaded to a client album stays hidden, so the photographer still curates before revealing")
    void shouldKeepUploadedFileHiddenInClientAlbum() {
        // Given - a client album keeps the curate-then-reveal default
        Album album = Album.createForClient("Sesja", photographer, client);
        File file = File.create(new FileName("surowe.jpg"), 100L, "image/jpeg");

        // When
        album.addFile(file);

        // Then
        assertFalse(album.getPhotos().get(file.getFileId()).isVisible());
    }

    @Test
    @DisplayName("Admin can list files of any album")
    void shouldGrantAccessToAdminAlways() {
        // Given
        Album album = Album.createForClient("Private", photographer, client);

        // When & Then - the admin has access even when visible = false
        assertTrue(album.hasAccessToGetFilesFromAlbum(admin, false));
    }

    @Test
    @DisplayName("Client cannot request hidden files of an album")
    void shouldDenyAccessToClientWhenAlbumIsNotVisible() {
        // Given
        Album album = Album.createForClient("Secret", photographer, client);

        // When & Then
        assertThrows(DomainSecurityException.class, () -> album.hasAccessToGetFilesFromAlbum(client, false));
    }

    @Test
    @DisplayName("Watermark cannot be applied to a file with an unsupported extension")
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
    @DisplayName("Watermark flag is set on the selected files")
    void shouldSetWatermarkSuccessfully() {
        // Given - the happy path for the watermark
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
    @DisplayName("Watermark cannot be enabled when no platform logo is configured")
    void shouldThrowWhenEnablingWatermarkWithoutConfiguredPlatformWatermark() {
        // Given - no logo uploaded, so enabling the watermark must be blocked server-side
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
    @DisplayName("Watermark can always be removed, even when the platform logo is gone")
    void shouldAllowDisablingWatermarkEvenWhenPlatformWatermarkNotConfigured() {
        // Given - a flagged file; removing the watermark must not depend on the logo being present
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
    @DisplayName("Watermarking a mixed selection skips already watermarked files instead of failing the whole batch")
    void shouldBeIdempotentWhenSomeFilesAlreadyWatermarked() {
        // Given - one file is already watermarked, the other is not
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

        // When - a batch containing an already watermarked file must NOT throw
        album.changeWatermarkStatus(photographer, true, List.of(alreadyId, freshId), true);

        // Then - both are watermarked (the toggle is a pure flag flip; watermarked versions are
        // composed while serving, so there are no file events left to count)
        assertTrue(album.getPhotos().get(alreadyId).isHasWatermark());
        assertTrue(album.getPhotos().get(freshId).isHasWatermark());
    }

    @Test
    @DisplayName("A portfolio album refuses a watermark — the tiled mark guards undelivered client photos, it does not brand the showcase")
    void shouldRejectWatermarkOnPortfolioAlbum() {
        // Given - an admin album is the public portfolio
        Album album = Album.createForAdmin("Portfolio", admin);
        File photo = File.create(new FileName("promo.jpg"), 100L, "image/jpeg");
        FileId fileId = photo.getFileId();

        Map<FileId, File> photos = new HashMap<>();
        photos.put(fileId, photo);
        album.assignPhotosToAlbum(photos);

        // When & Then - a broken business rule (400), not an authorization denial
        assertThrows(AlbumException.class, () ->
                album.changeWatermarkStatus(admin, true, List.of(fileId), true));
        assertFalse(album.getPhotos().get(fileId).isHasWatermark());
    }

    @Test
    @DisplayName("A watermark flag left on a portfolio album can still be cleared, so it never blocks deleting the platform logo")
    void shouldAllowClearingWatermarkOnPortfolioAlbum() {
        // Given - a flag predating the rule, built directly: the domain no longer lets one in
        Album album = Album.createForAdmin("PortfolioLegacy", admin);
        File flagged = new File(new FileId(FileId.newId()), new FileName("legacy.jpg"), 100L,
                "image/jpeg", Instant.now(), true, true);
        FileId fileId = flagged.getFileId();

        Map<FileId, File> photos = new HashMap<>();
        photos.put(fileId, flagged);
        album.assignPhotosToAlbum(photos);

        // When - removing is allowed even on a portfolio album
        album.changeWatermarkStatus(admin, false, List.of(fileId), true);

        // Then
        assertFalse(album.getPhotos().get(fileId).isHasWatermark());
    }

    @Test
    @DisplayName("A client album still accepts a watermark, so the portfolio rule did not disable the feature everywhere")
    void shouldStillAllowWatermarkOnClientAlbum() {
        // Given - client proofs are the only place a watermark belongs
        Album album = Album.createForClient("ClientProofs", photographer, client);
        File photo = File.create(new FileName("proof.jpg"), 100L, "image/jpeg");
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
    @DisplayName("Storage size is converted from bytes to gigabytes")
    void shouldCalcToGBCorrectly() {
        // Given
        Album album = Album.createForAdmin("Math", admin);

        // When
        long expected = 1073741824L; // 1024 * 1024 * 1024

        // Then
        assertEquals(expected, album.calcToGB(1));
    }

    @Test
    @DisplayName("TTD cannot be set on an admin album because it has no client")
    void shouldThrowExceptionWhenAdminTriesToSetTTDForOwnAlbum() {
        // Given
        Album adminAlbum = Album.createForAdmin("SystemAlbum", admin);
        Instant futureDate = Instant.now().plusSeconds(360);

        // When & Then
        assertThrows(AlbumException.class, () -> adminAlbum.setTTD(futureDate, admin, admin.getEmail().value()));
    }

    @Test
    @DisplayName("TTD must be a date in the future")
    void shouldThrowExceptionWhenSettingTTDInThePast() {
        // Given
        Album album = Album.createForClient("ClientAlbum", photographer, client);
        Instant pastDate = Instant.now().minusSeconds(360);

        // When & Then
        assertThrows(AlbumException.class, () -> album.setTTD(pastDate, photographer, photographer.getEmail().value()));
    }

    @Test
    @DisplayName("A refused date and a refused user are different failures: one is a broken rule, the other a denial")
    void shouldSeparateBusinessRuleFromAuthorizationDenialOnSetTtd() {
        // Given - the same call fails for two unrelated reasons
        Album album = Album.createForClient("TtdSeparation", photographer, client);
        Instant futureDate = Instant.now().plusSeconds(3600);
        Instant pastDate = Instant.now().minusSeconds(3600);

        // When / Then - a past date is a broken business rule: fix the input and retry (400)
        assertThrows(AlbumException.class,
                () -> album.setTTD(pastDate, photographer, photographer.getEmail().value()));

        // When / Then - a client is simply not allowed to do this, whatever the date (403)
        assertThrows(DomainSecurityException.class,
                () -> album.setTTD(futureDate, client, client.getEmail().value()));
    }

    @Test
    @DisplayName("Photographer cannot set a TTD on an album that belongs to another photographer")
    void shouldDenyTtdWhenPhotographerDoesNotOwnTheAlbum() {
        // Given
        User otherPhotographer = User.create("Other", new Email("other-ttd@photodrive.pl"), dummyPassword, Role.PHOTOGRAPHER);
        Album album = Album.createForClient("ForeignTtd", photographer, client);
        Instant futureDate = Instant.now().plusSeconds(3600);

        // When / Then
        assertThrows(DomainSecurityException.class,
                () -> album.setTTD(futureDate, otherPhotographer, otherPhotographer.getEmail().value()));
        assertNull(album.getTtd());
    }

    @Test
    @DisplayName("Setting TTD stores the date and notifies the client")
    void shouldSetTTDSuccessfully() {
        // Given - the happy path for TTD
        Album album = Album.createForClient("ClientAlbumTTD", photographer, client);
        Instant futureDate = Instant.now().plusSeconds(3600);

        // When
        album.setTTD(futureDate, photographer, photographer.getEmail().value());

        // Then
        assertEquals(futureDate, album.getTtd());
    }

    @Test
    @DisplayName("Removing an album clears its photos")
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
    @DisplayName("Client cannot remove an album")
    void shouldThrowExceptionWhenClientTriesToRemoveAlbum() {
        // Given
        Album album = Album.createForClient("ClientTryDelete", photographer, client);

        // When & Then
        assertThrows(DomainSecurityException.class, () ->
                album.removeFolder(album, client, photographer.getEmail().value())
        );
    }

    @Test
    @DisplayName("An album that has not expired yet cannot be removed as expired")
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
    @DisplayName("Photographer can remove a file from his own album")
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
    @DisplayName("Removing a file that is not in the album is rejected")
    void shouldThrowWhenRemovingNonExistentFile() {
        // Given
        Album album = Album.createForClient("RemoveTest2", photographer, client);
        FileId nonExistent = new FileId(java.util.UUID.randomUUID());

        // When & Then
        assertThrows(AlbumException.class, () -> album.removeFiles(nonExistent, photographer));
    }

    @Test
    @DisplayName("Client cannot remove files, even from his own album")
    void shouldDenyClientRemovingFileFromOwnAlbum() {
        // Given
        Album album = Album.createForClient("RemoveTest3", photographer, client);
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        FileId fileId = file.getFileId();
        album.addFile(file);

        // When / Then
        assertThrows(DomainSecurityException.class, () -> album.removeFiles(fileId, client));
        assertTrue(album.getPhotos().containsKey(fileId));
    }

    // -----------------------------------------------------------------------
    // renameFile
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Photographer can rename a file in his own album")
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
    @DisplayName("Renaming to a name already taken in the album is rejected")
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
    @DisplayName("Client cannot rename files, even in his own album")
    void shouldDenyClientRenamingFileInOwnAlbum() {
        // Given
        Album album = Album.createForClient("RenameDeny", photographer, client);
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        album.addFile(file);

        // When / Then
        assertThrows(DomainSecurityException.class,
                () -> album.renameFile(file.getFileId(), new FileName("new.jpg"), client));
        assertEquals(new FileName("photo.jpg"), album.getPhotos().get(file.getFileId()).getFileName());
    }

    // -----------------------------------------------------------------------
    // changeFileVisibleStatus
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Photographer can reveal a file to the client")
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
    @DisplayName("Client cannot change file visibility")
    void shouldDenyClientChangingVisibilityInOwnAlbum() {
        // Given
        Album album = Album.createForClient("VisibleDeny", photographer, client);
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        album.addFile(file);

        // When / Then
        assertThrows(DomainSecurityException.class, () ->
                album.changeFileVisibleStatus(List.of(file.getFileId()), true, client, client.getEmail()));
        assertFalse(album.getPhotos().get(file.getFileId()).isVisible());
    }

    @Test
    @DisplayName("Changing visibility of a mixed selection skips files already in the target state")
    void shouldBeIdempotentWhenSomeFilesAlreadyVisible() {
        // Given - a mixed selection: one file is already visible, the other is not
        Album album = Album.createForClient("MixedVisible", photographer, client);
        File alreadyVisible = File.create(new FileName("a.jpg"), 100L, "image/jpeg");
        File notVisible = File.create(new FileName("b.jpg"), 100L, "image/jpeg");
        album.addFile(alreadyVisible);
        album.addFile(notVisible);
        album.changeFileVisibleStatus(List.of(alreadyVisible.getFileId()), true, photographer, photographer.getEmail());

        // When - the same action on a batch containing an already visible file must NOT throw
        album.changeFileVisibleStatus(
                List.of(alreadyVisible.getFileId(), notVisible.getFileId()), true, photographer, photographer.getEmail());

        // Then - both are visible
        assertTrue(album.getPhotos().get(alreadyVisible.getFileId()).isVisible());
        assertTrue(album.getPhotos().get(notVisible.getFileId()).isVisible());
    }

    @Test
    @DisplayName("Visibility event counts only newly revealed files, so the client is not mailed about unchanged ones")
    void shouldReportOnlyNewlyChangedCountInVisibilityEvent() {
        // Given - two files, one already visible
        Album album = Album.createForClient("VisibleEventCount", photographer, client);
        File a = File.create(new FileName("a.jpg"), 100L, "image/jpeg");
        File b = File.create(new FileName("b.jpg"), 100L, "image/jpeg");
        album.addFile(a);
        album.addFile(b);
        album.changeFileVisibleStatus(List.of(a.getFileId()), true, photographer, photographer.getEmail());
        album.pullDomainEvents(); // drop the event produced by the setup

        // When - batch [a (already visible), b (new)]: only b actually changes
        album.changeFileVisibleStatus(
                List.of(a.getFileId(), b.getFileId()), true, photographer, photographer.getEmail());

        // Then - the event reports 1 (not 2), so the client is not mailed about files he could already see
        FileVisibleStatusChanged event = album.pullDomainEvents().stream()
                .filter(FileVisibleStatusChanged.class::isInstance)
                .map(FileVisibleStatusChanged.class::cast)
                .findFirst().orElseThrow();
        assertEquals(1, event.sizeList());
    }

    @Test
    @DisplayName("No visibility event and no mail when nothing actually changed")
    void shouldNotEmitVisibilityEventWhenNothingChanged() {
        // Given - the file is already visible
        Album album = Album.createForClient("NoChangeEvent", photographer, client);
        File a = File.create(new FileName("a.jpg"), 100L, "image/jpeg");
        album.addFile(a);
        album.changeFileVisibleStatus(List.of(a.getFileId()), true, photographer, photographer.getEmail());
        album.pullDomainEvents();

        // When - revealing a file that is already visible
        album.changeFileVisibleStatus(List.of(a.getFileId()), true, photographer, photographer.getEmail());

        // Then - no event at all, so the client gets no pointless mail
        assertTrue(album.pullDomainEvents().stream()
                .noneMatch(FileVisibleStatusChanged.class::isInstance));
    }

    // -----------------------------------------------------------------------
    // swapFiles
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Swapping removes the files from the source album and hands them to the target")
    void shouldSwapFilesToTargetMapSuccessfully() {
        // Given - two portfolio albums: the only pair allowed to exchange photos
        Album source = Album.createForAdmin("Source", admin);
        File file = File.create(new FileName("swap.jpg"), 100L, "image/jpeg");
        FileId fileId = file.getFileId();
        source.addFile(file);

        Album target = Album.createForAdmin("Target", admin);

        // When
        List<File> removedFiles = source.swapFiles(admin, target.getAlbumPath(), List.of(fileId));
        java.util.Map<FileId, File> incomingFiles = new java.util.LinkedHashMap<>();
        for (File f : removedFiles) {
            incomingFiles.put(f.getFileId(), f);
        }
        target.receiveFiles(incomingFiles, source);

        // Then
        assertFalse(source.getPhotos().containsKey(fileId));
        assertTrue(target.getPhotos().containsKey(fileId));
    }

    @Test
    @DisplayName("A client's photo cannot be moved into the portfolio, so private material never lands one click away from the public site")
    void shouldRejectSwapFromClientAlbumIntoPortfolio() {
        // Given - this was also the backdoor that carried a hasWatermark flag into the portfolio,
        // bypassing changeWatermarkStatus (receiveFiles moves File objects with their state).
        Album source = Album.createForClient("ClientSource", photographer, client);
        Album target = Album.createForAdmin("Portfolio", admin);
        File file = File.create(new FileName("private.jpg"), 100L, "image/jpeg");
        source.addFile(file);
        Map<FileId, File> incoming = Map.of(file.getFileId(), file);

        // When & Then
        assertThrows(AlbumException.class, () -> target.receiveFiles(incoming, source));
    }

    @Test
    @DisplayName("Portfolio photos cannot be pushed into a client album — an admin may look into client folders but never puts anything in")
    void shouldRejectSwapFromPortfolioIntoClientAlbum() {
        // Given
        Album source = Album.createForAdmin("Portfolio", admin);
        Album target = Album.createForClient("ClientTarget", photographer, client);
        File file = File.create(new FileName("promo.jpg"), 100L, "image/jpeg");
        source.addFile(file);
        Map<FileId, File> incoming = Map.of(file.getFileId(), file);

        // When & Then
        assertThrows(AlbumException.class, () -> target.receiveFiles(incoming, source));
    }

    @Test
    @DisplayName("Photos never travel between two client albums, so one client's session cannot leak into another client's gallery")
    void shouldRejectSwapBetweenTwoClientAlbums() {
        // Given - two different clients of the same photographer
        User otherClient = User.create("Other Client", new Email("client2@photodrive.pl"), dummyPassword, Role.CLIENT);
        Album source = Album.createForClient("ClientA", photographer, client);
        Album target = Album.createForClient("ClientB", photographer, otherClient);
        File file = File.create(new FileName("session.jpg"), 100L, "image/jpeg");
        source.addFile(file);
        Map<FileId, File> incoming = Map.of(file.getFileId(), file);

        // When & Then - a misfiled photo is fixed by delete + re-upload, never by moving client material
        assertThrows(AlbumException.class, () -> target.receiveFiles(incoming, source));
    }

    @Test
    @DisplayName("Client cannot move files between albums")
    void shouldThrowWhenClientTriesToSwapFiles() {
        // Given
        Album album = Album.createForClient("SwapDeny", photographer, client);
        File file = File.create(new FileName("photo.jpg"), 100L, "image/jpeg");
        FileId fileId = file.getFileId();
        album.addFile(file);

        // When & Then
        assertThrows(DomainSecurityException.class,
                () -> album.swapFiles(client, new pl.photodrive.core.domain.vo.AlbumPath("other/album"), List.of(fileId)));
    }

    @Test
    @DisplayName("Photographer cannot move files out of an album he does not own")
    void shouldThrowWhenNonOwnerPhotographerTriesToSwap() {
        // Given - the album belongs to `photographer`, another photographer attempts the swap
        User otherPhotographer = User.create("Other", new Email("other@photodrive.pl"), dummyPassword, Role.PHOTOGRAPHER);
        Album album = Album.createForClient("OwnedSource", photographer, client);
        File file = File.create(new FileName("owned.jpg"), 100L, "image/jpeg");
        album.addFile(file);

        // When & Then - no ownership means refusal (broken-object-level-authorization hardening)
        assertThrows(DomainSecurityException.class,
                () -> album.swapFiles(otherPhotographer, new pl.photodrive.core.domain.vo.AlbumPath("other/album"), List.of(file.getFileId())));
    }

    @Test
    @DisplayName("Target album rejects an incoming file whose name is taken, so no photo is silently overwritten")
    void shouldThrowWhenReceivingFileWithDuplicateNameInTarget() {
        // Given - BOTH albums are portfolio albums on purpose: only then does the swap rule let the
        // transfer through, so the NAME COLLISION is what actually decides. With client albums this
        // test would pass vacuously — receiveFiles would reject the transfer itself.
        Album source = Album.createForAdmin("SourceDup", admin);
        Album target = Album.createForAdmin("TargetDup", admin);
        File existing = File.create(new FileName("dup.jpg"), 100L, "image/jpeg");
        target.addFile(existing);

        File incoming = File.create(new FileName("dup.jpg"), 100L, "image/jpeg");
        Map<FileId, File> incomingFiles = new HashMap<>();
        incomingFiles.put(incoming.getFileId(), incoming);

        // When & Then - the name collision is rejected instead of silently overwriting a photo
        assertThrows(AlbumException.class, () -> target.receiveFiles(incomingFiles, source));
    }

    // -----------------------------------------------------------------------
    // getFilePath
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Storage path of an admin album is the album name alone")
    void shouldReturnJustAlbumNameForAdmin() {
        // Given
        Album album = Album.createForAdmin("AdminAlbum", admin);

        // When
        String path = album.getFilePath(admin, admin.getEmail().value());

        // Then
        assertEquals("AdminAlbum", path);
    }

    @Test
    @DisplayName("Storage path of a client album is prefixed with the photographer email")
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
    @DisplayName("Client resolves the same storage path as the photographer who owns the album")
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
    @DisplayName("Admin can manage the content of any album")
    void shouldGrantAccessToAdminAlbum() {
        // Given
        Album album = Album.createForClient("CanAccessTest", photographer, client);

        // When / Then - admin always has access
        assertTrue(album.canAccess(admin.getId(), admin.getRoles()));
    }

    @Test
    @DisplayName("Photographer can manage the content of his own album")
    void shouldGrantAccessToOwnerPhotographer() {
        // Given
        Album album = Album.createForClient("OwnerAccess", photographer, client);

        // When / Then
        assertTrue(album.canAccess(photographer.getId(), photographer.getRoles()));
    }

    @Test
    @DisplayName("Photographer cannot manage another photographer's album")
    void shouldDenyAccessToOtherPhotographer() {
        // Given
        Album album = Album.createForClient("OtherPhotograph", photographer, client);
        User other = User.create("Other", new Email("other@photodrive.pl"), dummyPassword, Role.PHOTOGRAPHER);

        // When / Then
        assertFalse(album.canAccess(other.getId(), other.getRoles()));
    }

    @Test
    @DisplayName("Client cannot manage album content")
    void shouldDenyAccessToClient() {
        // Given
        Album album = Album.createForClient("ClientNoAccess", photographer, client);

        // When / Then
        assertFalse(album.canAccess(client.getId(), client.getRoles()));
    }

    @Test
    @DisplayName("Client can read only the files marked visible in his album")
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
    @DisplayName("Public portfolio exposes only the files marked visible")
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
