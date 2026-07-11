package pl.photodrive.core.infrastructure.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.photodrive.core.infrastructure.jpa.entity.PlatformWatermarkEntity;

public interface PlatformWatermarkJpaRepository extends JpaRepository<PlatformWatermarkEntity, Integer> {
}
