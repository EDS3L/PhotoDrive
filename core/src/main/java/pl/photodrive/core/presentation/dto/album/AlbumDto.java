package pl.photodrive.core.presentation.dto.album;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AlbumDto(
        UUID albumId,
        String name,
        UUID photographId,
        UUID clientId,
        Instant ttd,
        List<FileDto> files,
        boolean isPublic
) {
}
