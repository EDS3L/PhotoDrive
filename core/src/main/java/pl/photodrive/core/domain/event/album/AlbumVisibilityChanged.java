package pl.photodrive.core.domain.event.album;

import java.util.UUID;

public record AlbumVisibilityChanged(UUID albumId, boolean isPublic) {
}
