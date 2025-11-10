package pl.photodrive.core.infrastructure.jpa.vo.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;


@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserIdEmbeddable {

    @Column(name = "userId", columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID value;

}
