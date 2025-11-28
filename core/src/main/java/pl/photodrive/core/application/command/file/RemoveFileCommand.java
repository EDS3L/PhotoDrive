package pl.photodrive.core.application.command.file;

import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.FileId;

public record RemoveFileCommand(FileId fileId, AlbumId albumId) {
}
