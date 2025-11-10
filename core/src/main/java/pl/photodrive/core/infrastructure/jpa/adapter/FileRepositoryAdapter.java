package pl.photodrive.core.infrastructure.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.photodrive.core.domain.port.repository.FileRepository;
import pl.photodrive.core.domain.vo.FileName;
import pl.photodrive.core.infrastructure.jpa.repository.FileJpaRepository;
import pl.photodrive.core.infrastructure.jpa.vo.file.FileNameEmbeddable;

@Repository
@RequiredArgsConstructor
public class FileRepositoryAdapter implements FileRepository {

    private final FileJpaRepository jpa;

    @Override
    public boolean existsByFileName(FileName fileName) {
        return jpa.existsByFileName(new FileNameEmbeddable(fileName.value()));
    }
}
