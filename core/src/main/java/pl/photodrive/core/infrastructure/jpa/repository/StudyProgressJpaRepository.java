package pl.photodrive.core.infrastructure.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.photodrive.core.infrastructure.jpa.entity.StudyProgressEntity;

public interface StudyProgressJpaRepository extends JpaRepository<StudyProgressEntity, String> {
}
