package pl.photodrive.core.infrastructure.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.photodrive.core.domain.vo.UserId;
import pl.photodrive.core.infrastructure.jpa.entity.UserEntity;
import pl.photodrive.core.infrastructure.jpa.vo.EmailEmbeddable;


public interface UserJpaRepository extends JpaRepository<UserEntity, UserId> {

    boolean existsByEmail(EmailEmbeddable email);
}
