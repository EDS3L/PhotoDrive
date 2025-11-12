package pl.photodrive.core.application.command.album;

import java.util.List;

public record DownloadFilesCommand(String albumName, List<String> fileNames) {
}
