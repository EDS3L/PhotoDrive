package pl.photodrive.core.presentation.dto.album;

import java.util.UUID;

public record FileDto(UUID fileID, String fileName, long sizeBytes, String contentType) {
}
