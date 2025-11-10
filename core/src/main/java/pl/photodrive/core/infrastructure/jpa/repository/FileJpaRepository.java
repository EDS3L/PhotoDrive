package pl.photodrive.core.infrastructure.jpa.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import pl.photodrive.core.infrastructure.jpa.entity.FileEntity;
import pl.photodrive.core.infrastructure.jpa.vo.file.FileIdEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.file.FileNameEmbeddable;

public interface FileJpaRepository extends JpaRepository<FileEntity, FileIdEmbeddable> {

    boolean existsByFileName(FileNameEmbeddable fileNameEmbeddable);

}
