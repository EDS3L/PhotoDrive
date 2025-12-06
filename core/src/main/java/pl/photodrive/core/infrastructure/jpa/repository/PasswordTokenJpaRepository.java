package pl.photodrive.core.infrastructure.jpa.repository;

import org.hibernate.validator.constraints.UniqueElements;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.photodrive.core.infrastructure.jpa.entity.PasswordTokenEntity;
import pl.photodrive.core.infrastructure.jpa.vo.passwordToken.PasswordTokenIdEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.user.UserIdEmbeddable;

public interface PasswordTokenJpaRepository extends JpaRepository<PasswordTokenEntity, PasswordTokenIdEmbeddable> {
    boolean existsByUserId(@UniqueElements UserIdEmbeddable userId);

    PasswordTokenEntity findByUserId(@UniqueElements UserIdEmbeddable userId);
}
