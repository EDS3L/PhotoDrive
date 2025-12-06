package pl.photodrive.core.infrastructure.jpa.mapper;

import pl.photodrive.core.domain.model.File;
import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.domain.vo.FileName;
import pl.photodrive.core.infrastructure.jpa.entity.FileEntity;
import pl.photodrive.core.infrastructure.jpa.vo.file.FileIdEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.file.FileNameEmbeddable;

public class FileEntityMapper {

    public static File toDomain(FileEntity entity) {
        return new File((new FileId(entity.getFileId().getValue())),
               new FileName(entity.getFileName().getValue()),
                entity.getSizeBytes(),
                entity.getContentType(),
                entity.getUploadedAt(),
                entity.isVisible(),
                entity.isHasWatermark());
    }

    public static FileEntity toEntity(File file) {
        return  FileEntity.builder()
                .fileId(new FileIdEmbeddable(file.getFileId().value()))
                .fileName(new FileNameEmbeddable(file.getFileName().value()))
                .sizeBytes(file.getSizeBytes())
                .contentType(file.getContentType())
                .uploadedAt(file.getUploadedAt())
                .isVisible(file.isVisible())
                .hasWatermark(file.isHasWatermark())
                .build();
    }
}
