package pl.photodrive.core.presentation.dto.file;

import java.util.Map;//

public record UploadResponse(Map<String, String> files, String message) {
}
