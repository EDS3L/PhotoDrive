package pl.photodrive.core.infrastructure.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.photodrive.core.domain.model.File;
import pl.photodrive.core.domain.port.repository.FileRepository;
import pl.photodrive.core.domain.vo.FileName;
import pl.photodrive.core.infrastructure.jpa.mapper.FileEntityMapper;
import pl.photodrive.core.infrastructure.jpa.repository.FileJpaRepository;
import pl.photodrive.core.infrastructure.jpa.vo.file.FileNameEmbeddable;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FileRepositoryAdapter implements FileRepository {

    private final FileJpaRepository jpa;

    @Override
    public File save(File file) {
        return FileEntityMapper.toDomain(jpa.save(FileEntityMapper.toEntity(file)));
    }

    @Override
    public Optional<File> findByFileName(FileName fileName) {
        return jpa.findByFileName(new FileNameEmbeddable(fileName.value())).map(FileEntityMapper::toDomain);
    }
}
