package pl.photodrive.core.presentation.dto.file;

import java.util.List;

public record UploadResponse(List<String> fileIds, String message) {}
