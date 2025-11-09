package pl.photodrive.core.infrastructure.jpa.vo.user;

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
public class UserIdEmbeddable {

    @Column(columnDefinition = "BINARY(16)", name = "userId")
    private UUID value;


}
