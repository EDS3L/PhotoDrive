package pl.photodrive.core.presentation.dto.album;

import pl.photodrive.core.domain.model.File;
import pl.photodrive.core.domain.vo.AlbumPath;
import pl.photodrive.core.domain.vo.FileId;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AlbumDto(UUID albumId, String name, UUID photographId, UUID clientId, Instant ttd,
                       Map<FileId, File> photos, AlbumPath albumPath) {
}
