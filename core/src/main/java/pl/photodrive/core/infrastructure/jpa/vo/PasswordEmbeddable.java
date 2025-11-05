package pl.photodrive.core.infrastructure.jpa.vo;


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
public class PasswordEmbeddable {

    @Column(name = "password", nullable = false)
    private String value;
}
