package pl.photodrive.core.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.photodrive.core.application.command.file.FileResource;
import pl.photodrive.core.application.service.AlbumManagementService;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.presentation.dto.album.PublicAlbumDto;
import pl.photodrive.core.presentation.dto.album.PublicAlbumPhotosResponse;
import pl.photodrive.core.presentation.dto.album.PublicPhotoDto;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/album")
@RequiredArgsConstructor
public class PublicAlbumController {

    private final AlbumManagementService albumService;

    @GetMapping("/all")
    public ResponseEntity<List<PublicAlbumDto>> getAllPublicAlbums() {
        List<PublicAlbumDto> albums = albumService.getAllPublicAlbums().stream()
                .map(album -> new PublicAlbumDto(
                        album.getAlbumId().value(),
                        album.getName(),
                        album.getDisplayName(),
                        (int) album.getPhotos().values().stream().filter(f -> f.isVisible()).count()))
                .toList();
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=30, must-revalidate")
                .body(albums);
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<PublicAlbumPhotosResponse> getPublicPhotosByAlbumName(@PathVariable String name) {
        Album album = albumService.getPublicAlbumByName(name);
        List<PublicPhotoDto> photos = album.getPhotos().values().stream()
                .filter(file -> file.isVisible())
                .map(file -> new PublicPhotoDto(file.getFileId().value(), file.getFileName().value()))
                .toList();
        PublicAlbumPhotosResponse response = new PublicAlbumPhotosResponse(
                album.getAlbumId().value(), album.getName(), photos);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=30, must-revalidate")
                .body(response);
    }

    @GetMapping("/{albumId}/photos")
    public ResponseEntity<List<PublicPhotoDto>> getPublicPhotos(@PathVariable UUID albumId) {
        Album album = albumService.getPublicAlbum(new AlbumId(albumId));
        List<PublicPhotoDto> photos = album.getPhotos().values().stream()
                .filter(file -> file.isVisible())
                .map(file -> new PublicPhotoDto(file.getFileId().value(), file.getFileName().value()))
                .toList();
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=30, must-revalidate")
                .body(photos);
    }

    /**
     * {@code width} to życzenie klienta co do rozmiaru — serwer i tak zacina je na twardym limicie
     * (`AlbumManagementService.PUBLIC_MAX_DIMENSION`), więc przez ten endpoint nie da się pobrać
     * oryginału. Brak parametru = wariant o maksymalnym dozwolonym rozmiarze.
     */
    @GetMapping("/{albumId}/photo/{fileName}")
    public ResponseEntity<Resource> getPublicPhoto(@PathVariable UUID albumId,
                                                   @PathVariable String fileName,
                                                   @RequestParam(required = false) Integer width) {
        FileResource fileResponse = albumService.getPublicPhoto(albumId, fileName, width);
        String contentDisposition = ContentDisposition.inline()
                .filename(fileResponse.resource().getFilename(), StandardCharsets.UTF_8)
                .build()
                .toString();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileResponse.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(fileResponse.resource());
    }
}
