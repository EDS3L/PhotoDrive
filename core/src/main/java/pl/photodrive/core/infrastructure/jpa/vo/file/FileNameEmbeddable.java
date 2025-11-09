package pl.photodrive.core.infrastructure.jpa.vo.file;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileNameEmbeddable {

    @Column(name = "fileName", nullable = false)
    private String value;
}
