package pl.photodrive.core.presentation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.photodrive.core.application.command.file.FileResource;
import pl.photodrive.core.application.service.AlbumManagementService;
import pl.photodrive.core.domain.exception.AlbumNotFoundException;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.model.File;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.FileName;
import pl.photodrive.core.domain.vo.HashedPassword;
import pl.photodrive.core.infrastructure.jwt.JwtAuthenticationFilter;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The portfolio is reachable WITHOUT logging in, so the crucial property is that it exposes
 * only what the photographer deliberately marked as visible.
 */
@WebMvcTest(controllers = PublicAlbumController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
class PublicAlbumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlbumManagementService albumService;

    /** Public album with one visible and one hidden file. */
    private Album albumWithVisibleAndHiddenFile() {
        User admin = User.create("Admin", new Email("admin@photodrive.pl"),
                new HashedPassword("hashed"), Role.ADMIN);
        Album album = Album.createForAdmin("portfolio-sluby", admin);

        File visible = File.create(new FileName("widoczne.jpg"), 10L, "image/jpeg");
        File hidden = File.create(new FileName("ukryte.jpg"), 10L, "image/jpeg");
        // Admin-album uploads are visible by default (B.5), so the "hidden" one is hidden explicitly.
        album.addFile(visible);
        album.addFile(hidden);
        album.changeFileVisibleStatus(List.of(hidden.getFileId()), false, admin, admin.getEmail());
        album.pullDomainEvents();
        return album;
    }

    @Test
    @DisplayName("Portfolio counts only visible photos, so hidden ones do not inflate the number")
    void shouldListPublicAlbumsWithVisiblePhotoCountOnly() throws Exception {
        // Given
        given(albumService.getAllPublicAlbums()).willReturn(List.of(albumWithVisibleAndHiddenFile()));

        // When / Then
        mockMvc.perform(get("/api/public/album/all"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "public, max-age=30, must-revalidate"))
                .andExpect(jsonPath("$[0].name").value("portfolio-sluby"))
                // counts ONLY visible photos - a hidden one must not inflate the number
                .andExpect(jsonPath("$[0].photoCount").value(1));
    }

    @Test
    @DisplayName("Listing a portfolio album by name exposes only visible photos")
    void shouldNotLeakHiddenPhotosWhenListingByAlbumName() throws Exception {
        // Given
        given(albumService.getPublicAlbumByName("portfolio-sluby"))
                .willReturn(albumWithVisibleAndHiddenFile());

        // When / Then
        mockMvc.perform(get("/api/public/album/by-name/portfolio-sluby"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos.length()").value(1))
                .andExpect(jsonPath("$.photos[0].fileName").value("widoczne.jpg"));
    }

    @Test
    @DisplayName("Listing a portfolio album by id exposes only visible photos")
    void shouldNotLeakHiddenPhotosWhenListingById() throws Exception {
        // Given
        Album album = albumWithVisibleAndHiddenFile();
        given(albumService.getPublicAlbum(any(AlbumId.class))).willReturn(album);

        // When / Then
        mockMvc.perform(get("/api/public/album/{id}/photos", album.getAlbumId().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fileName").value("widoczne.jpg"));
    }

    @Test
    @DisplayName("Portfolio photo is served inline with a long cache lifetime")
    void shouldServePublicPhoto() throws Exception {
        // Given
        Album album = albumWithVisibleAndHiddenFile();
        stubPhotoResponse();

        // When / Then
        mockMvc.perform(get("/api/public/album/{id}/photo/{name}", album.getAlbumId().value(), "widoczne.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string("Cache-Control", "public, max-age=86400"));

        // Then - no size asked for: the service decides, and it caps (A9)
        then(albumService).should().getPublicPhoto(any(), eq("widoczne.jpg"), isNull());
    }

    @Test
    @DisplayName("The requested photo size reaches the service, which is what lets the grid ask for thumbnails")
    void shouldPassRequestedWidthToTheService() throws Exception {
        // Given
        Album album = albumWithVisibleAndHiddenFile();
        stubPhotoResponse();

        // When / Then
        mockMvc.perform(get("/api/public/album/{id}/photo/{name}", album.getAlbumId().value(), "widoczne.jpg")
                        .param("width", "800"))
                .andExpect(status().isOk());

        then(albumService).should().getPublicPhoto(any(), eq("widoczne.jpg"), eq(800));
    }

    private void stubPhotoResponse() {
        given(albumService.getPublicPhoto(any(), anyString(), any()))
                .willReturn(new FileResource(new ByteArrayResource("bytes".getBytes()) {
                    @Override
                    public String getFilename() {
                        return "widoczne.jpg";
                    }
                }, "image/jpeg"));
    }

    @Test
    @DisplayName("A private album returns 404 on the public endpoint")
    void shouldReturn404ForNonPublicAlbum() throws Exception {
        // Given
        given(albumService.getPublicAlbumByName("prywatny"))
                .willThrow(new AlbumNotFoundException("Public album not found"));

        // When / Then
        mockMvc.perform(get("/api/public/album/by-name/prywatny"))
                .andExpect(status().isNotFound());
    }
}
