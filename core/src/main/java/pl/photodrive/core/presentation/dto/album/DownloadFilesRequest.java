package pl.photodrive.core.presentation.dto.album;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DownloadFilesRequest(@NotEmpty List<String> fileList, String albumName) {
}
