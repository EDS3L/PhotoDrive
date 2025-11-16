package pl.photodrive.core.application.command.album;

import pl.photodrive.core.domain.vo.AlbumId;

import java.util.List;

public record DownloadFilesCommand(List<String> fileNames, AlbumId albumId) {
}
