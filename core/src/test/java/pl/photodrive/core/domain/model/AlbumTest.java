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
import pl.photodrive.core.domain.vo.Password;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AlbumTest {

    private User admin;
    private User photographer;
    private User client;


    private final Password dummyPassword = new Password("Test1234!");

    @BeforeEach
    void setUp() {
        admin = User.create("Admin", new Email("admin@photodrive.pl"), dummyPassword, Role.ADMIN, "Test1234!");
        photographer = User.create("Photographer", new Email("photographer@photodrive.pl"), dummyPassword, Role.PHOTOGRAPHER, "Test1234!");
        client = User.create("Client", new Email("client@photodrive.pl"), dummyPassword, Role.CLIENT, "Test1234!");
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
}