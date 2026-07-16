package pl.photodrive.core.presentation.dto.album;

import java.util.UUID;

/** {@code displayName} może być null — front pokazuje wtedy techniczną nazwę albumu. */
public record PublicAlbumDto(UUID albumId, String name, String displayName, int photoCount) {
}
