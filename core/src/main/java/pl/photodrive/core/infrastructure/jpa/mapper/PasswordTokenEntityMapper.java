package pl.photodrive.core.infrastructure.jpa.mapper;

import pl.photodrive.core.domain.model.PasswordToken;
import pl.photodrive.core.domain.vo.PasswordTokenId;
import pl.photodrive.core.domain.vo.UserId;
import pl.photodrive.core.infrastructure.jpa.entity.PasswordTokenEntity;
import pl.photodrive.core.infrastructure.jpa.vo.passwordToken.PasswordTokenIdEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.user.UserIdEmbeddable;

public class PasswordTokenEntityMapper {

    public static PasswordToken toDomain(PasswordTokenEntity entity) {
        return new PasswordToken(new PasswordTokenId(entity.getPasswordTokenId().getValue()),
                entity.getToken(),
                entity.getExpiration(),
                entity.getCreated(),
                new UserId(entity.getUserId().getValue()));
    }

    public static PasswordTokenEntity toEntity(PasswordToken passwordToken) {
        return PasswordTokenEntity.builder()
                .passwordTokenId(new PasswordTokenIdEmbeddable(passwordToken.getId().value()))
                .token(passwordToken.getToken())
                .expiration(passwordToken.getExpiration())
                .created(passwordToken.getCreated())
                .userId(new UserIdEmbeddable(passwordToken.getUserId().value()))
                .build();
    }

}
