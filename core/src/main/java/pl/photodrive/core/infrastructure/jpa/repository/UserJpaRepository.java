package pl.photodrive.core.infrastructure.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.photodrive.core.infrastructure.jpa.entity.UserEntity;
import pl.photodrive.core.infrastructure.jpa.vo.user.EmailEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.user.UserIdEmbeddable;

import java.util.Optional;


public interface UserJpaRepository extends JpaRepository<UserEntity, UserIdEmbeddable> {
    boolean existsByEmail(EmailEmbeddable email);

    Optional<UserEntity> findByEmail(EmailEmbeddable emailEmbeddable);
}
