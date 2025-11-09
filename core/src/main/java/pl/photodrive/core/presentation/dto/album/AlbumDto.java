package pl.photodrive.core.presentation.dto.album;

import java.util.UUID;

public record AlbumDto(UUID albumId, String name, UUID photographId, UUID clientId) {
}
