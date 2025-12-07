package pl.photodrive.core.presentation.dto.file;

import java.util.List;

public record UploadResponse(List<UploadResponseFile> responseFile, String message) {
}
