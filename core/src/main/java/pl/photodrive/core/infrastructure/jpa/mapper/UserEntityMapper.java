package pl.photodrive.core.infrastructure.jpa.mapper;

import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.Password;
import pl.photodrive.core.domain.vo.UserId;
import pl.photodrive.core.infrastructure.jpa.entity.UserEntity;
import pl.photodrive.core.infrastructure.jpa.vo.user.EmailEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.user.PasswordEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.user.UserIdEmbeddable;

import java.util.ArrayList;
import java.util.List;

public class UserEntityMapper {
    public static User toDomain(UserEntity entity) {
        List<UserId> assignedUserIds = entity.getAssignedUsers().stream().map(embeddable -> new UserId(embeddable.getValue())).toList();

        return new User(new UserId(entity.getUserId().getValue()),
                entity.getName(),
                new Email(entity.getEmail().getValue()),
                new Password(entity.getPassword().getValue()),
                entity.getRoles(),
                entity.isChangePasswordOnNextLogin(),
                entity.isActive(),
                assignedUserIds);
    }

    public static UserEntity toEntity(User user) {
        List<UserIdEmbeddable> assignedUserEmbeddable = new ArrayList<>();
        if (user.getAssignedUsers() != null) {
            assignedUserEmbeddable = user.getAssignedUsers().stream().map(domainId -> new UserIdEmbeddable(domainId.value())).toList();
        }

        return UserEntity.builder().userId(new UserIdEmbeddable(user.getId().value())).name(user.getName()).email(new EmailEmbeddable(
                user.getEmail().value())).password(new PasswordEmbeddable(user.getPassword().value())).roles(user.getRoles()).changePasswordOnNextLogin(
                user.isChangePasswordOnNextLogin()).isActive(user.isActive()).assignedUsers(assignedUserEmbeddable).build();
    }
}
