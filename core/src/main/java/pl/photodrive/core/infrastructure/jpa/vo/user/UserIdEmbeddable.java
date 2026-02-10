package pl.photodrive.core.infrastructure.jpa.vo.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;


@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data

public class UserIdEmbeddable {

    @Column(name = "userId", columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID value;

}
