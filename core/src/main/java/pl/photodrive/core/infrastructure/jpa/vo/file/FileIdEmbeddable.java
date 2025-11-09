package pl.photodrive.core.infrastructure.jpa.vo.file;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileIdEmbeddable {

    @Column(columnDefinition = "BINARY(16)", name = "fileId")
    private UUID value;
}
