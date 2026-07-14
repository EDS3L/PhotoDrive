package pl.photodrive.core.presentation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import pl.photodrive.core.application.port.repository.AlbumRepository;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.support.IntegrationTest;
import pl.photodrive.core.support.TestFixtures;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testy integracyjne {@code AlbumController} — jedyne miejsce, gdzie sprawdzamy CAŁY tor:
 * HTTP → filtry bezpieczeństwa → kontroler → domena → zdarzenia → **prawdziwa baza MySQL**
 * i **prawdziwy dysk**. Testy jednostkowe serwisu weryfikują, że wołany jest właściwy port;
 * dopiero tutaj widać, czy plik faktycznie leży na dysku, wiersz w bazie, a klient nie
 * zobaczy zdjęcia, którego nie powinien.
 *
 * <p>Ten kontroler był wcześniej <b>nietestowany i wykluczony z pomiaru pokrycia</b> (5.1).
 */
class AlbumControllerIT extends IntegrationTest {

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // --- tworzenie albumów -------------------------------------------------

    @Test
    @DisplayName("Photographer's new client album lands in the database and gets its folder on disk")
    void shouldPersistAlbumAndCreateFolderWhenPhotographerCreatesClientAlbum() throws Exception {
        // Given
        User photographer = fixtures.photographer("foto@photodrive.dev");
        User client = fixtures.client("klient@photodrive.dev");

        // When
        MvcResult result = mockMvc.perform(post("/api/album/client/{clientId}/create", client.getId().value())
                        .cookie(fixtures.authCookie(photographer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Sesja plenerowa"}"""))
                .andExpect(status().isOk())
                .andReturn();

        // Then - the album is really in the database, not just echoed back in the response
        UUID albumId = UUID.fromString(json(result).get("albumId").asText());
        Album saved = loadAlbum(new AlbumId(albumId));
        assertThat(saved.getPhotographId()).isEqualTo(photographer.getId().value());
        assertThat(saved.getClientId()).isEqualTo(client.getId().value());
        // Name carries the client's e-mail, so two clients can have a "Sesja plenerowa" each
        assertThat(saved.getName()).startsWith("Sesja plenerowa_klient@photodrive.dev_");

        // ...and the folder for the photos exists (BEFORE_COMMIT event handler did its job)
        assertThat(albumFolder(saved)).exists().isDirectory();
    }

    @Test
    @DisplayName("Client cannot create an album, even by calling the endpoint directly")
    void shouldRejectAlbumCreationWhenCallerIsClient() throws Exception {
        // Given
        User client = fixtures.client("klient@photodrive.dev");

        // When / Then - the domain rule holds even though the URL is not role-guarded in WebConfig
        mockMvc.perform(post("/api/album/client/{clientId}/create", client.getId().value())
                        .cookie(fixtures.authCookie(client))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Nieautoryzowany"}"""))
                .andExpect(status().isForbidden());

        assertThat(inTransaction(albumRepository::findAll)).isEmpty();
    }

    @Test
    @DisplayName("A request without a session cookie never reaches the album data")
    void shouldRejectRequestWhenNoAuthenticationCookie() throws Exception {
        // Given - no cookie at all
        // When / Then
        mockMvc.perform(get("/api/album/all"))
                .andExpect(status().isUnauthorized());
    }

    // --- upload ------------------------------------------------------------

    @Test
    @DisplayName("Uploaded photos are written to disk and recorded in the database")
    void shouldStorePhotosOnDiskAndInDatabaseWhenUploading() throws Exception {
        // Given
        User photographer = fixtures.photographer("foto@photodrive.dev");
        User client = fixtures.client("klient@photodrive.dev");
        Album album = createClientAlbum(photographer, client, "Sesja slubna");

        // When
        mockMvc.perform(multipart("/api/album/upload/{albumId}/files", album.getAlbumId().value())
                        .file(photo("plaza.jpg"))
                        .file(photo("las.jpg"))
                        .cookie(fixtures.authCookie(photographer)))
                .andExpect(status().isAccepted());

        // Then - metadata in MySQL...
        Album reloaded = loadAlbum(album.getAlbumId());
        assertThat(reloaded.getPhotos().values())
                .extracting(file -> file.getFileName().value())
                .containsExactlyInAnyOrder("plaza.jpg", "las.jpg");

        // ...and the binaries on disk (the whole point of splitting storage from the database)
        assertThat(albumFolder(reloaded).resolve("plaza.jpg")).exists();
        assertThat(albumFolder(reloaded).resolve("las.jpg")).exists();
    }

    @Test
    @DisplayName("A photo uploaded under a taken name is renamed, so the existing one is never overwritten")
    void shouldGiveUniqueNameWhenUploadedFileNameIsAlreadyTaken() throws Exception {
        // Given - an album that already holds foto.jpg
        User photographer = fixtures.photographer("foto@photodrive.dev");
        User client = fixtures.client("klient@photodrive.dev");
        Album album = createClientAlbum(photographer, client, "Chrzciny");
        upload(album, photographer, photo("foto.jpg"));

        // When - the same name arrives again
        upload(album, photographer, photo("foto.jpg"));

        // Then - both photos survive; the newcomer gets a suffix (backend format: "foto (1).jpg")
        List<String> names = fileNames(album, photographer);
        assertThat(names).hasSize(2).contains("foto.jpg");
        assertThat(names).anyMatch(name -> !name.equals("foto.jpg"));
    }

    @Test
    @DisplayName("Album name with Polish diacritics is rejected — a documented limitation, not a feature")
    void shouldRejectAlbumNameWithDiacritics() throws Exception {
        // Given - "Ślub" (wedding) is arguably THE most common album name for a Polish photographer
        User photographer = fixtures.photographer("foto@photodrive.dev");
        User client = fixtures.client("klient@photodrive.dev");

        // When / Then - AlbumPath allows ASCII only, so the album cannot be created at all
        // (the path is derived from the name). Test pins the CURRENT behaviour; the fix is
        // tracked as B.32 — when it lands, this test flips to expect success.
        mockMvc.perform(post("/api/album/client/{clientId}/create", client.getId().value())
                        .cookie(fixtures.authCookie(photographer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ślub"}"""))
                .andExpect(status().isBadRequest());
    }

    // --- widoczność i dostęp klienta ---------------------------------------

    @Test
    @DisplayName("Freshly uploaded photos stay hidden until the photographer publishes them")
    void shouldKeepUploadedPhotosHiddenFromClientUntilMadeVisible() throws Exception {
        // Given - photos uploaded, but not yet reviewed by the photographer
        User photographer = fixtures.photographer("foto@photodrive.dev");
        User client = fixtures.client("klient@photodrive.dev");
        Album album = createClientAlbum(photographer, client, "Sesja");
        upload(album, photographer, photo("surowe.jpg"));

        // When - the client opens their album straight away
        MvcResult result = mockMvc.perform(get("/api/album/getAllAssignedAlbums")
                        .cookie(fixtures.authCookie(client)))
                .andExpect(status().isOk())
                .andReturn();

        // Then - nothing leaks before the photographer decides; the default is privacy, not exposure
        assertThat(json(result).get(0).get("files")).isEmpty();
        mockMvc.perform(get("/api/album/{albumId}/photo/{name}", album.getAlbumId().value(), "surowe.jpg")
                        .cookie(fixtures.authCookie(client)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Client sees only the photos made visible, and never learns who the photographer is")
    void shouldExposeOnlyVisiblePhotosAndHideOwnershipWhenCallerIsClient() throws Exception {
        // Given - two photos, one of them hidden by the photographer
        User photographer = fixtures.photographer("foto@photodrive.dev");
        User client = fixtures.client("klient@photodrive.dev");
        Album album = createClientAlbum(photographer, client, "Sesja");
        upload(album, photographer, photo("wybrane.jpg"), photo("odrzut.jpg"));
        // Photos arrive hidden; the photographer publishes only the one he picked
        setVisible(album, photographer, fileIdOf(album, photographer, "wybrane.jpg"), true);

        // When - the client lists their albums
        MvcResult result = mockMvc.perform(get("/api/album/getAllAssignedAlbums")
                        .cookie(fixtures.authCookie(client)))
                .andExpect(status().isOk())
                .andReturn();

        // Then - the rejected shot stays invisible...
        JsonNode clientAlbum = json(result).get(0);
        assertThat(clientAlbum.get("files")).hasSize(1);
        assertThat(clientAlbum.get("files").get(0).get("fileName").asText()).isEqualTo("wybrane.jpg");
        // ...and the client view leaks no internal ids
        assertThat(clientAlbum.get("photographId").isNull()).isTrue();
        assertThat(clientAlbum.get("clientId").isNull()).isTrue();
    }

    @Test
    @DisplayName("Client can open a visible photo but is refused a hidden one")
    void shouldServeVisiblePhotoAndRefuseHiddenOneWhenCallerIsClient() throws Exception {
        // Given
        User photographer = fixtures.photographer("foto@photodrive.dev");
        User client = fixtures.client("klient@photodrive.dev");
        Album album = createClientAlbum(photographer, client, "Sesja");
        upload(album, photographer, photo("wybrane.jpg"), photo("odrzut.jpg"));
        setVisible(album, photographer, fileIdOf(album, photographer, "wybrane.jpg"), true);
        Cookie clientCookie = fixtures.authCookie(client);

        // When / Then - the visible one is served as an image...
        mockMvc.perform(get("/api/album/{albumId}/photo/{name}", album.getAlbumId().value(), "wybrane.jpg")
                        .cookie(clientCookie))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));

        // ...while guessing the file name of a hidden photo gets the client nowhere
        mockMvc.perform(get("/api/album/{albumId}/photo/{name}", album.getAlbumId().value(), "odrzut.jpg")
                        .cookie(clientCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Client of one album cannot read another client's album")
    void shouldRefuseAccessWhenClientAsksForForeignAlbum() throws Exception {
        // Given - two clients of the same photographer
        User photographer = fixtures.photographer("foto@photodrive.dev");
        User owner = fixtures.client("wlasciciel@photodrive.dev");
        User stranger = fixtures.client("obcy@photodrive.dev");
        Album album = createClientAlbum(photographer, owner, "Prywatna sesja");
        upload(album, photographer, photo("intymne.jpg"));

        // When / Then - knowing the album UUID is not enough
        mockMvc.perform(get("/api/album/{albumId}/file-names", album.getAlbumId().value())
                        .cookie(fixtures.authCookie(stranger)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/album/{albumId}/photo/{name}", album.getAlbumId().value(), "intymne.jpg")
                        .cookie(fixtures.authCookie(stranger)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Photographer cannot touch an album belonging to another photographer")
    void shouldRefuseAccessWhenPhotographerAsksForForeignAlbum() throws Exception {
        // Given
        User owner = fixtures.photographer("wlasciciel@photodrive.dev");
        User intruder = fixtures.photographer("obcy@photodrive.dev");
        User client = fixtures.client("klient@photodrive.dev");
        Album album = createClientAlbum(owner, client, "Nie twoja sesja");

        // When / Then
        mockMvc.perform(get("/api/album/{albumId}/file-names", album.getAlbumId().value())
                        .cookie(fixtures.authCookie(intruder)))
                .andExpect(status().isForbidden());
    }

    // --- publikacja --------------------------------------------------------

    @Test
    @DisplayName("Publishing an album to the public portfolio is reserved for the admin")
    void shouldAllowOnlyAdminToPublishAlbum() throws Exception {
        // Given - an admin album (the only kind that can be published)
        User admin = fixtures.admin("admin@photodrive.dev");
        User photographer = fixtures.photographer("foto@photodrive.dev");
        Album album = createAdminAlbum(admin, "portfolio-sluby");

        // When - the photographer tries to publish it
        mockMvc.perform(patch("/api/album/{albumId}/setPublic", album.getAlbumId().value())
                        .param("isPublic", "true")
                        .cookie(fixtures.authCookie(photographer)))
                // Then - blocked by the URL rule in WebConfig, before any domain code runs
                .andExpect(status().isForbidden());
        assertThat(loadAlbum(album.getAlbumId()).isPublic()).isFalse();

        // When - the admin does it
        mockMvc.perform(patch("/api/album/{albumId}/setPublic", album.getAlbumId().value())
                        .param("isPublic", "true")
                        .cookie(fixtures.authCookie(admin)))
                .andExpect(status().isOk());

        // Then - the flag is persisted, so the public portfolio will pick the album up
        assertThat(loadAlbum(album.getAlbumId()).isPublic()).isTrue();
    }

    // --- ZIP ---------------------------------------------------------------

    @Test
    @DisplayName("Download returns a ZIP with exactly the requested photos")
    void shouldPackOnlyRequestedPhotosIntoZip() throws Exception {
        // Given - three photos in the album
        User photographer = fixtures.photographer("foto@photodrive.dev");
        User client = fixtures.client("klient@photodrive.dev");
        Album album = createClientAlbum(photographer, client, "Sesja");
        upload(album, photographer, photo("a.jpg"), photo("b.jpg"), photo("c.jpg"));
        for (String name : List.of("a.jpg", "b.jpg", "c.jpg")) {
            setVisible(album, photographer, fileIdOf(album, photographer, name), true);
        }

        // When - the client asks for two of them
        MvcResult result = mockMvc.perform(post("/api/album/{albumId}/download", album.getAlbumId().value())
                        .cookie(fixtures.authCookie(client))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fileList":["a.jpg","c.jpg"]}"""))
                .andExpect(status().isOk())
                .andReturn();

        // Then - the archive holds those two and nothing else
        assertThat(zipEntryNames(result.getResponse().getContentAsByteArray()))
                .containsExactlyInAnyOrder("a.jpg", "c.jpg");
    }

    // --- rename / swap / delete -------------------------------------------

    @Test
    @DisplayName("Renaming a photo renames it in the database and on disk, keeping the two in step")
    void shouldRenameFileInDatabaseAndOnDisk() throws Exception {
        // Given
        User photographer = fixtures.photographer("foto@photodrive.dev");
        User client = fixtures.client("klient@photodrive.dev");
        Album album = createClientAlbum(photographer, client, "Sesja");
        upload(album, photographer, photo("dsc_0001.jpg"));
        UUID fileId = fileIdOf(album, photographer, "dsc_0001.jpg");

        // When
        mockMvc.perform(put("/api/album/{albumId}/rename/{fileId}", album.getAlbumId().value(), fileId)
                        .cookie(fixtures.authCookie(photographer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newFileName":"plaza-zachod.jpg"}"""))
                .andExpect(status().isNoContent());

        // Then - a rename that stops halfway (DB only) would break every future download
        assertThat(fileNames(album, photographer)).containsExactly("plaza-zachod.jpg");
        Path folder = albumFolder(loadAlbum(album.getAlbumId()));
        assertThat(folder.resolve("plaza-zachod.jpg")).exists();
        assertThat(folder.resolve("dsc_0001.jpg")).doesNotExist();
    }

    @Test
    @DisplayName("Swapping moves the photo to the target album, in the database and on disk")
    void shouldMoveFileBetweenAlbumsWhenSwapping() throws Exception {
        // Given - two albums of the same photographer
        User photographer = fixtures.photographer("foto@photodrive.dev");
        User client = fixtures.client("klient@photodrive.dev");
        Album source = createClientAlbum(photographer, client, "Zrodlo");
        Album target = createClientAlbum(photographer, client, "Cel");
        upload(source, photographer, photo("zdjecie.jpg"));
        UUID fileId = fileIdOf(source, photographer, "zdjecie.jpg");

        // When
        mockMvc.perform(patch("/api/album/{albumId}/album/{targetId}/swap",
                        source.getAlbumId().value(), target.getAlbumId().value())
                        .cookie(fixtures.authCookie(photographer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fileIdList":["%s"]}""".formatted(fileId)))
                .andExpect(status().isOk());

        // Then - the photo belongs to the target album now...
        assertThat(fileNames(source, photographer)).isEmpty();
        assertThat(fileNames(target, photographer)).containsExactly("zdjecie.jpg");

        // ...and the file physically moved, instead of being duplicated or left behind
        Album reloadedTarget = loadAlbum(target.getAlbumId());
        Album reloadedSource = loadAlbum(source.getAlbumId());
        assertThat(albumFolder(reloadedTarget).resolve("zdjecie.jpg")).exists();
        assertThat(albumFolder(reloadedSource).resolve("zdjecie.jpg")).doesNotExist();
    }

    @Test
    @DisplayName("Deleting an album removes its row, its photos and its folder")
    void shouldRemoveAlbumRowAndFolderWhenDeleting() throws Exception {
        // Given
        User photographer = fixtures.photographer("foto@photodrive.dev");
        User client = fixtures.client("klient@photodrive.dev");
        Album album = createClientAlbum(photographer, client, "Do skasowania");
        upload(album, photographer, photo("zdjecie.jpg"));
        Path folder = albumFolder(loadAlbum(album.getAlbumId()));
        assertThat(folder).exists();

        // When
        mockMvc.perform(delete("/api/album/{albumId}/delete", album.getAlbumId().value())
                        .cookie(fixtures.authCookie(photographer)))
                .andExpect(status().isNoContent());

        // Then - no orphaned row and no orphaned gigabytes on the VPS
        assertThat(inTransaction(() -> albumRepository.findByAlbumId(album.getAlbumId()))).isEmpty();
        assertThat(folder).doesNotExist();
    }

    @Test
    @DisplayName("Setting a TTD stores the deletion date the scheduler will act on")
    void shouldPersistTtdWhenPhotographerSetsIt() throws Exception {
        // Given
        User photographer = fixtures.photographer("foto@photodrive.dev");
        User client = fixtures.client("klient@photodrive.dev");
        Album album = createClientAlbum(photographer, client, "Sesja");

        // When
        mockMvc.perform(patch("/api/album/{albumId}/setTtd", album.getAlbumId().value())
                        .cookie(fixtures.authCookie(photographer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ttd":"2030-01-01"}"""))
                .andExpect(status().isOk());

        // Then - without persistence the album would live forever, contrary to what the client was told
        assertThat(loadAlbum(album.getAlbumId()).getTtd()).isNotNull();
    }

    // --- helpers -----------------------------------------------------------

    private Album createClientAlbum(User photographer, User client, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/album/client/{clientId}/create", client.getId().value())
                        .cookie(fixtures.authCookie(photographer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}""".formatted(name)))
                .andExpect(status().isOk())
                .andReturn();
        UUID albumId = UUID.fromString(json(result).get("albumId").asText());
        return loadAlbum(new AlbumId(albumId));
    }

    private Album createAdminAlbum(User admin, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/album/admin/create")
                        .cookie(fixtures.authCookie(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}""".formatted(name)))
                .andExpect(status().isOk())
                .andReturn();
        UUID albumId = UUID.fromString(json(result).get("albumId").asText());
        return loadAlbum(new AlbumId(albumId));
    }

    /** Odczyt agregatu w transakcji — mapowanie encja→domena jest leniwe (patrz {@code IntegrationTest}). */
    private Album loadAlbum(AlbumId albumId) {
        return inTransaction(() -> albumRepository.findByAlbumId(albumId))
                .orElseThrow(() -> new AssertionError("Album " + albumId.value() + " is not in the database"));
    }

    private void upload(Album album, User uploader, MockMultipartFile... files) throws Exception {
        var request = multipart("/api/album/upload/{albumId}/files", album.getAlbumId().value());
        Stream.of(files).forEach(request::file);
        mockMvc.perform(request.cookie(fixtures.authCookie(uploader)))
                .andExpect(status().isAccepted());
    }

    private void setVisible(Album album, User user, UUID fileId, boolean visible) throws Exception {
        mockMvc.perform(patch("/api/album/{albumId}/files/setVisible", album.getAlbumId().value())
                        .param("visible", String.valueOf(visible))
                        .cookie(fixtures.authCookie(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idList":["%s"]}""".formatted(fileId)))
                .andExpect(status().isOk());
    }

    private List<String> fileNames(Album album, User user) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/album/{albumId}/file-names", album.getAlbumId().value())
                        .cookie(fixtures.authCookie(user)))
                .andExpect(status().isOk())
                .andReturn();
        List<String> names = new ArrayList<>();
        json(result).forEach(node -> names.add(node.asText()));
        return names;
    }

    private UUID fileIdOf(Album album, User user, String fileName) {
        Album reloaded = loadAlbum(album.getAlbumId());
        return reloaded.getPhotos().values().stream()
                .filter(file -> file.getFileName().value().equals(fileName))
                .map(file -> file.getFileId().value())
                .findFirst()
                .orElseThrow(() -> new AssertionError("No file named " + fileName + " in the album"));
    }

    private static MockMultipartFile photo(String name) {
        return new MockMultipartFile("files", name, MediaType.IMAGE_JPEG_VALUE, TestFixtures.jpeg(800, 600));
    }

    private static Path albumFolder(Album album) {
        return storageRoot().resolve(album.getAlbumPath().value());
    }

    private static List<String> zipEntryNames(byte[] zip) throws Exception {
        List<String> names = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        return names;
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }
}
