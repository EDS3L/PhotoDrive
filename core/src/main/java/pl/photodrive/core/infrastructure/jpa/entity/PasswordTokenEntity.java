package pl.photodrive.core.infrastructure.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import pl.photodrive.core.infrastructure.jpa.vo.passwordToken.PasswordTokenIdEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.user.UserIdEmbeddable;

import java.time.Instant;

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
    @Column(name = "token", length = 64, nullable = false)
    private String token;
    private Instant expiration;
    private Instant created;
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "user_id", unique = true))
    private UserIdEmbeddable userId;
}
