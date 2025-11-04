package pl.photodrive.core.infrastructure.jpa.entity;


import jakarta.persistence.*;
import lombok.*;
import pl.photodrive.core.domain.vo.Email;

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
    private Email email;
    private String password;

}
