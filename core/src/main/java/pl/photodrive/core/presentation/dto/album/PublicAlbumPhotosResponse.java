package pl.photodrive.core.presentation.dto.album;

import java.util.List;
import java.util.UUID;

public record PublicAlbumPhotosResponse(UUID albumId, String name, List<PublicPhotoDto> photos) {
}
