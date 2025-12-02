package pl.photodrive.core.infrastructure.jpa.vo.album;

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
public class AlbumPathEmbeddable {

    @Column(name = "albumPath", nullable = false)
    private String value;
}
