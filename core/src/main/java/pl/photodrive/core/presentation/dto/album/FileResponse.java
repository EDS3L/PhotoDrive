package pl.photodrive.core.presentation.dto.album;

import pl.photodrive.core.domain.model.File;

public record FileResponse(String fileId, String fileName, long sizeBytes, String contentType) {
    static FileResponse fromDomain(File file) {
        return new FileResponse(file.getFileId().value().toString(),
                file.getFileName().value(),
                file.getSizeBytes(),
                file.getContentType());
    }
}