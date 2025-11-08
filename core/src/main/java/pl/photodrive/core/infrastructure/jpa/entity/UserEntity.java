package pl.photodrive.core.infrastructure.jpa.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.infrastructure.jpa.vo.EmailEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.PasswordEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.UserIdEmbeddable;

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
    @NotEmpty
    @Size(min=1)
    private Set<Role> roles;

}
