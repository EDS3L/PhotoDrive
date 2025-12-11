package pl.photodrive.core.application.command.album;

import java.util.List;
import java.util.UUID;

public record DownloadFilesCommand(List<String> fileNames, UUID albumId) {
}
