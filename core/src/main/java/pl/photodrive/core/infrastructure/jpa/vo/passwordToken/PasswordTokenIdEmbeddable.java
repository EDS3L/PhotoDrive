package pl.photodrive.core.infrastructure.jpa.vo.passwordToken;

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
public class PasswordTokenIdEmbeddable {

    @Column(columnDefinition = "VARCHAR(36)", name = "passwordTokenId")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID value;
}
