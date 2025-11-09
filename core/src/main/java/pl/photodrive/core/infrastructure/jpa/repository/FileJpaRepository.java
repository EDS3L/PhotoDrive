package pl.photodrive.core.infrastructure.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.photodrive.core.domain.model.File;
import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.infrastructure.jpa.entity.FileEntity;
import pl.photodrive.core.infrastructure.jpa.vo.file.FileNameEmbeddable;

import java.util.Optional;

public interface FileJpaRepository extends JpaRepository<FileEntity, FileId> {

    Optional<FileEntity> findByFileName(FileNameEmbeddable fileName);
}
