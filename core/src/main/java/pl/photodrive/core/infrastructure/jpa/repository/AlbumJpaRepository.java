package pl.photodrive.core.infrastructure.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.photodrive.core.infrastructure.jpa.entity.AlbumEntity;
import pl.photodrive.core.infrastructure.jpa.vo.album.AlbumIdEmbeddable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlbumJpaRepository extends JpaRepository<AlbumEntity, AlbumIdEmbeddable> {

    AlbumEntity findByPhotographId(UUID photographId);

    Optional<AlbumEntity> findByAlbumIdAndIsPublicTrue(AlbumIdEmbeddable albumId);

    List<AlbumEntity> findByClientId(UUID clientId);

    List<AlbumEntity> findAllByPhotographId(UUID photographId);

    List<AlbumEntity> findAllByClientId(UUID clientId);

    List<AlbumEntity> findByTtdBeforeAndTtdIsNotNull(Instant now);

    Optional<AlbumEntity> findByName(String name);

    boolean existsByName(String name);

    List<AlbumEntity> findAllByIsPublicTrue();

    Optional<AlbumEntity> findByNameAndIsPublicTrue(String name);

}
