package pl.photodrive.core.infrastructure.jpa.entity;

import jakarta.persistence.*;
import lombok.*;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.infrastructure.jpa.vo.user.EmailEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.user.PasswordEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.user.UserIdEmbeddable;

import java.util.List;
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
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "userId", referencedColumnName = "userId"))
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles;
    boolean changePasswordOnNextLogin;
    boolean isActive;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "assigned_users", joinColumns = @JoinColumn(name = "photographer_user_id", referencedColumnName = "userId"))
    @AttributeOverrides({@AttributeOverride(name = "id", column = @Column(name = "assigned_users")), @AttributeOverride(name = "version", column = @Column(name = "assigned_users_version"))})
    private List<UserIdEmbeddable> assignedUsers;

}
