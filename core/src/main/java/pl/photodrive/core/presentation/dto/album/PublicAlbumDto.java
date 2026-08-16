package pl.photodrive.core.presentation.dto.album;

import java.util.UUID;

public record PublicAlbumDto(UUID albumId, String name, String displayName, int photoCount) {
}
