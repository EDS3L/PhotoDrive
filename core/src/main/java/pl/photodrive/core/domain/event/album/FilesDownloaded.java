package pl.photodrive.core.domain.event.album;

import java.util.List;

public record FilesDownloaded(String albumName, List<String> fileNames, String photographEmail) {
}
