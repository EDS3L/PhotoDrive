package pl.photodrive.core.infrastructure.jpa.entity;


import jakarta.persistence.*;
import lombok.*;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.infrastructure.jpa.vo.EmailEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.PasswordEmbeddable;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    private String name;
    @Embedded
    private EmailEmbeddable email;
    @Embedded
    private PasswordEmbeddable password;
    private Set<Role> roles;

}
