package pl.photodrive.core.presentation.dto.album;

import pl.photodrive.core.domain.model.Album;

import java.util.List;

public record AlbumResponse(String albumId, String name, String photographerId, String clientId,
                            List<FileResponse> files) {
    public static AlbumResponse fromDomain(Album album) {
        return new AlbumResponse(album.getAlbumId().value().toString(),
                album.getName(),
                album.getPhotographId().toString(),
                album.getClientId() != null ? album.getClientId().toString() : null,
                album.getPhotos().values().stream().map(FileResponse::fromDomain).toList());
    }
}