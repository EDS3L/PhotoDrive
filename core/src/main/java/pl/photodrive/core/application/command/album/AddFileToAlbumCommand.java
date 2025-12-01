package pl.photodrive.core.application.command.album;

import pl.photodrive.core.domain.vo.AlbumId;

import java.util.List;
import java.util.UUID;

public record AddFileToAlbumCommand(UUID albumId, List<FileUpload> fileUploads) {
}
