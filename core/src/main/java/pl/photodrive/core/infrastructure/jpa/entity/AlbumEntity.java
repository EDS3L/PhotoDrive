package pl.photodrive.core.infrastructure.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pl.photodrive.core.domain.model.File;
import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.infrastructure.jpa.vo.album.AlbumIdEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.album.AlbumPathEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.file.FileIdEmbeddable;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "albums")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumEntity {

    @EmbeddedId
    private AlbumIdEmbeddable albumId;
    private String name;
    @Column(columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID photographId;
    @Column(columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID clientId;

    @Builder.Default
    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    @MapKey(name = "fileId")
    private Map<FileIdEmbeddable, FileEntity> photos = new LinkedHashMap<>();
    private Instant ttd;
    @Embedded
    private AlbumPathEmbeddable albumPath;
}
