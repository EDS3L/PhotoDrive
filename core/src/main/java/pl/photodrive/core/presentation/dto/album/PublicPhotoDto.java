package pl.photodrive.core.presentation.dto.album;

import java.util.UUID;

public record PublicPhotoDto(UUID fileId, String fileName) {
}
