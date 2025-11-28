package pl.photodrive.core.application.command.file;

import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.domain.vo.FileName;

public record RenameFileCommand(AlbumId albumId, FileId fileId, FileName newFileName) {
}
