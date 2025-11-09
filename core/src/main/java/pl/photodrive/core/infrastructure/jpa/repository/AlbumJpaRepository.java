package pl.photodrive.core.infrastructure.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.infrastructure.jpa.entity.AlbumEntity;
import pl.photodrive.core.infrastructure.jpa.vo.album.AlbumIdEmbeddable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlbumJpaRepository extends JpaRepository<AlbumEntity, AlbumId> {

    List<AlbumEntity> findByPhotographId(UUID photographId);
    List<AlbumEntity> findByClientId(UUID clientId);
}
