package pl.photodrive.core.infrastructure.jpa.mapper;

import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.infrastructure.jpa.entity.UserEntity;

public class UserEntityMapper {
    public static User toDomain(UserEntity entity) {
        return new User(entity.getId(),entity.getName(),entity.getEmail(), entity.getPassword());
    }

    public static UserEntity toEntity(User user) {
        return UserEntity.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .password(user.getPassword())
                .build();
    }
}
