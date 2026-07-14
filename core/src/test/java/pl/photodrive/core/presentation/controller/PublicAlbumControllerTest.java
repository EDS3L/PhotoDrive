package pl.photodrive.core.presentation.controller;

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
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Portfolio jest dostępne BEZ logowania — kluczowe jest to, że wycieka wyłącznie to,
 * co fotograf świadomie oznaczył jako widoczne.
 */
@WebMvcTest(controllers = PublicAlbumController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
class PublicAlbumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlbumManagementService albumService;

    /** Album publiczny z jednym plikiem widocznym i jednym ukrytym. */
    private Album albumWithVisibleAndHiddenFile() {
        User admin = User.create("Admin", new Email("admin@photodrive.pl"),
                new HashedPassword("hashed"), Role.ADMIN);
        Album album = Album.createForAdmin("portfolio-sluby", admin);

        File visible = File.create(new FileName("widoczne.jpg"), 10L, "image/jpeg");
        File hidden = File.create(new FileName("ukryte.jpg"), 10L, "image/jpeg");
        album.addFile(visible);
        album.addFile(hidden);
        album.changeFileVisibleStatus(List.of(visible.getFileId()), true, admin, admin.getEmail());
        album.pullDomainEvents();
        return album;
    }

    @Test
    void shouldListPublicAlbumsWithVisiblePhotoCountOnly() throws Exception {
        given(albumService.getAllPublicAlbums()).willReturn(List.of(albumWithVisibleAndHiddenFile()));

        mockMvc.perform(get("/api/public/album/all"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "public, max-age=30, must-revalidate"))
                .andExpect(jsonPath("$[0].name").value("portfolio-sluby"))
                // liczy TYLKO widoczne — ukryte zdjęcie nie może podbijać licznika
                .andExpect(jsonPath("$[0].photoCount").value(1));
    }

    @Test
    void shouldNotLeakHiddenPhotosWhenListingByAlbumName() throws Exception {
        given(albumService.getPublicAlbumByName("portfolio-sluby"))
                .willReturn(albumWithVisibleAndHiddenFile());

        mockMvc.perform(get("/api/public/album/by-name/portfolio-sluby"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos.length()").value(1))
                .andExpect(jsonPath("$.photos[0].fileName").value("widoczne.jpg"));
    }

    @Test
    void shouldNotLeakHiddenPhotosWhenListingById() throws Exception {
        Album album = albumWithVisibleAndHiddenFile();
        given(albumService.getPublicAlbum(any(AlbumId.class))).willReturn(album);

        mockMvc.perform(get("/api/public/album/{id}/photos", album.getAlbumId().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fileName").value("widoczne.jpg"));
    }

    @Test
    void shouldServePublicPhoto() throws Exception {
        Album album = albumWithVisibleAndHiddenFile();
        given(albumService.getPublicPhoto(any(), anyString()))
                .willReturn(new FileResource(new ByteArrayResource("bytes".getBytes()) {
                    @Override
                    public String getFilename() {
                        return "widoczne.jpg";
                    }
                }, "image/jpeg"));

        mockMvc.perform(get("/api/public/album/{id}/photo/{name}", album.getAlbumId().value(), "widoczne.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string("Cache-Control", "public, max-age=86400"));
    }

    @Test
    void shouldReturn404ForNonPublicAlbum() throws Exception {
        given(albumService.getPublicAlbumByName("prywatny"))
                .willThrow(new AlbumNotFoundException("Public album not found"));

        mockMvc.perform(get("/api/public/album/by-name/prywatny"))
                .andExpect(status().isNotFound());
    }
}
