package pl.photodrive.core.infrastructure.jpa.mapper;

import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.Password;
import pl.photodrive.core.infrastructure.jpa.entity.UserEntity;
import pl.photodrive.core.infrastructure.jpa.vo.EmailEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.PasswordEmbeddable;

import java.util.Collections;

public class UserEntityMapper {
    public static User toDomain(UserEntity entity) {
        return new User(entity.getId(),
                entity.getName(),
                new Email(entity.getEmail().getValue()),
                new Password(entity.getPassword().getValue()),
                entity.getRoles());
    }

    public static UserEntity toEntity(User user) {
        return UserEntity.builder()
                .id(user.getId())
                .name(user.getName())
                .email(new EmailEmbeddable(user.getEmail().value()))
                .password(new PasswordEmbeddable(user.getPassword().value()))
                .roles(user.getRoles())
                .build();
    }
}
