package pl.photodrive.core.application.command.file;

import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.FileId;

import java.util.UUID;

public record RemoveFileCommand(UUID fileId, UUID albumId) {
}
