package pl.photodrive.core.infrastructure.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.photodrive.core.application.port.repository.AlbumRepository;
import pl.photodrive.core.application.port.repository.PublicAlbumSummary;
import pl.photodrive.core.domain.exception.AlbumException;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.infrastructure.jpa.mapper.AlbumEntityMapper;
import pl.photodrive.core.infrastructure.jpa.repository.AlbumJpaRepository;
import pl.photodrive.core.infrastructure.jpa.vo.album.AlbumIdEmbeddable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
//!
@Repository
@RequiredArgsConstructor
public class AlbumRepositoryAdapter implements AlbumRepository {

    private final AlbumJpaRepository jpa;

    @Override
    public Album save(Album album) {
        var savedEntity = jpa.save(AlbumEntityMapper.toEntity(album));
        return AlbumEntityMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Album> findByAlbumId(AlbumId albumId) {
        return jpa.findById(new AlbumIdEmbeddable(albumId.value())).map(AlbumEntityMapper::toDomain);
    }

    @Override
    public Optional<Album> findPublicByAlbumId(AlbumId albumId) {
        return jpa.findByAlbumIdAndIsPublicTrue(new AlbumIdEmbeddable(albumId.value())).map(AlbumEntityMapper::toDomain);
    }

    @Override
    public Album findByPhotographId(UUID photographId) {
        var entity = jpa.findByPhotographId(photographId);
        if (entity == null) {
            throw new AlbumException("Album for photographer " + photographId + " not found");
        }
        return AlbumEntityMapper.toDomain(entity);
    }

    @Override
    public Optional<Album> findByName(String name) {
        return jpa.findByName(name).map(AlbumEntityMapper::toDomain);
    }

    @Override
    public List<Album> findByClientId(UUID clientId) {
        return jpa.findByClientId(clientId).stream().map(AlbumEntityMapper::toDomain).toList();
    }

    @Override
    public boolean existsByName(String name) {
        return jpa.existsByName(name);
    }


    @Override
    public void delete(Album album) {
        jpa.delete(AlbumEntityMapper.toEntity(album));
    }

    @Override
    public List<Album> findAll() {
        return jpa.findAll().stream().map(AlbumEntityMapper::toDomain).toList();
    }

    @Override
    public List<Album> findAllByPhotographId(UUID photographId) {
        return jpa.findAllByPhotographId(photographId).stream().map(AlbumEntityMapper::toDomain).toList();
    }

    @Override
    public List<Album> findAllByClientId(UUID clientId) {
        return jpa.findAllByClientId(clientId).stream().map(AlbumEntityMapper::toDomain).toList();
    }

    @Override
    public List<Album> findByTtdBeforeAndTtdIsNotNull(Instant now) {
        return jpa.findByTtdBeforeAndTtdIsNotNull(now).stream().map(AlbumEntityMapper::toDomain).toList();
    }

    @Override
    public List<PublicAlbumSummary> findAllPublicSummaries() {
        return jpa.findPublicAlbumSummaries().stream()
                .map(view -> new PublicAlbumSummary(view.getAlbumId(),
                        view.getName(),
                        view.getDisplayName(),
                        // Wiersze sprzed kolumny display_order mają NULL — traktujemy jak 0.
                        view.getDisplayOrder() == null ? 0 : view.getDisplayOrder(),
                        view.getVisibleCount() == null ? 0 : view.getVisibleCount()))
                .toList();
    }

    @Override
    public Optional<Album> findPublicByName(String name) {
        return jpa.findByNameAndIsPublicTrue(name).map(AlbumEntityMapper::toDomain);
    }
}
