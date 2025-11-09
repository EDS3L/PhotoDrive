package pl.photodrive.core.infrastructure.jpa.entity;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import pl.photodrive.core.infrastructure.jpa.vo.file.FileIdEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.file.FileNameEmbeddable;

import java.time.Instant;
@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileEntity {

    @EmbeddedId
    private FileIdEmbeddable fileId;
    @Embedded
    private FileNameEmbeddable fileName;
    private long sizeBytes;
    private String contentType;
    private Instant uploadedAt;

}
