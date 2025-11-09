package pl.photodrive.core.infrastructure.jpa.vo.user;

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
public class EmailEmbeddable {

    @Column(name = "email", nullable = false)
    private String value;
}
