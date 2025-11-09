package pl.photodrive.core.infrastructure.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.port.repository.AlbumRepository;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.infrastructure.jpa.mapper.AlbumEntityMapper;
import pl.photodrive.core.infrastructure.jpa.repository.AlbumJpaRepository;
import pl.photodrive.core.infrastructure.jpa.vo.album.AlbumIdEmbeddable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AlbumRepositoryAdapter implements AlbumRepository {

    private final AlbumJpaRepository jpa;

    @Override
    public Album save(Album album) {
        return AlbumEntityMapper.toDomain(jpa.save(AlbumEntityMapper.toEntity(album)));
    }

    @Override
    public Optional<Album> findByAlbumId(AlbumId albumId) {
        return jpa.findById(new AlbumIdEmbeddable(albumId.value())).map(AlbumEntityMapper::toDomain);
    }

    @Override
    public List<Album> findByPhotographId(UUID photographerId) {
        return jpa.findByPhotographId(photographerId).stream().map(AlbumEntityMapper::toDomain).toList();
    }

    @Override
    public List<Album> findByClientId(UUID clientId) {
        return jpa.findByClientId(clientId).stream().map(AlbumEntityMapper::toDomain).toList();
    }
}
