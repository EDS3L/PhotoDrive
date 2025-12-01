package pl.photodrive.core.application.command.file;

import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.domain.vo.FileName;

import java.util.UUID;

public record RenameFileCommand(UUID albumId, UUID fileId, String newFileName) {
}
