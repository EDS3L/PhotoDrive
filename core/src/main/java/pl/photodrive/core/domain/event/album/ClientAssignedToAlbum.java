package pl.photodrive.core.domain.event.album;

import pl.photodrive.core.domain.vo.AlbumId;

import java.util.UUID;

public record ClientAssignedToAlbum(AlbumId albumId, UUID clientId) {
}
