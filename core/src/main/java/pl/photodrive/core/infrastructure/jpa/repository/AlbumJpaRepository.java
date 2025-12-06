package pl.photodrive.core.infrastructure.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.photodrive.core.infrastructure.jpa.entity.AlbumEntity;
import pl.photodrive.core.infrastructure.jpa.vo.album.AlbumIdEmbeddable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlbumJpaRepository extends JpaRepository<AlbumEntity, AlbumIdEmbeddable> {

    AlbumEntity findByPhotographId(UUID photographId);

    List<AlbumEntity> findByClientId(UUID clientId);

    Optional<AlbumEntity> findByName(String name);

    boolean existsByName(String name);

}
