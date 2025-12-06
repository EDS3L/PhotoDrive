package pl.photodrive.core.application.command.file;

import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.FileId;

import java.util.List;
import java.util.UUID;

public record RemoveFileCommand(List<UUID> fileIdList, UUID albumId) {
}
