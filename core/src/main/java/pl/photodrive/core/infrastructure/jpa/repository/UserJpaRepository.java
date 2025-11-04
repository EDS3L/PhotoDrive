package pl.photodrive.core.infrastructure.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.photodrive.core.infrastructure.jpa.entity.UserEntity;

import java.util.UUID;


public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
}
