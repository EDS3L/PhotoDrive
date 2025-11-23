package pl.photodrive.core.infrastructure.jpa.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.infrastructure.jpa.vo.user.EmailEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.user.PasswordEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.user.UserIdEmbeddable;

import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @EmbeddedId
    private UserIdEmbeddable userId;
    private String name;
    @Embedded
    private EmailEmbeddable email;
    @Embedded
    private PasswordEmbeddable password;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(
                    name = "userId",
                    referencedColumnName = "userId"
            )
    )
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles;
    boolean changePasswordOnNextLogin;
    boolean isActive;

}
