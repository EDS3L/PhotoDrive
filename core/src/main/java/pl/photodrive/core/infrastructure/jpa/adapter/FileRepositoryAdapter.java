package pl.photodrive.core.infrastructure.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.photodrive.core.application.port.repository.FileRepository;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.FileName;
import pl.photodrive.core.infrastructure.jpa.repository.FileJpaRepository;
import pl.photodrive.core.infrastructure.jpa.vo.album.AlbumIdEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.file.FileNameEmbeddable;

@Repository
@RequiredArgsConstructor
public class FileRepositoryAdapter implements FileRepository {

    private final FileJpaRepository jpa;

    @Override
    public boolean existsByAlbumIdAndFileName(AlbumId albumId, FileName fileName) {
        return jpa.existsByAlbumIdAndFileName(new AlbumIdEmbeddable(albumId.value()), new FileNameEmbeddable(fileName.value()));
    }
}
