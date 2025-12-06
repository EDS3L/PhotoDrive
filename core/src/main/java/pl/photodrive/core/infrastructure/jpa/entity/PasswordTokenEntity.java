package pl.photodrive.core.infrastructure.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pl.photodrive.core.infrastructure.jpa.vo.passwordToken.PasswordTokenIdEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.user.UserIdEmbeddable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "passwordTokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordTokenEntity {

    @EmbeddedId
    private PasswordTokenIdEmbeddable passwordTokenId;
    @Column(columnDefinition = "VARCHAR(36)", name = "token")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID token;
    private Instant expiration;
    private Instant created;
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "user_id", unique = true))
    private UserIdEmbeddable userId;
}
