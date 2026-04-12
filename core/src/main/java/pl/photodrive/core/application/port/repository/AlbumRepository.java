package pl.photodrive.core.application.port.repository;

import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.vo.AlbumId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlbumRepository {
    Album save(Album album);

    Optional<Album> findByAlbumId(AlbumId albumId);

    Optional<Album> findPublicByAlbumId(AlbumId albumId);

    Album findByPhotographId(UUID photographId);

    Optional<Album> findByName(String name);

    List<Album> findByClientId(UUID clientId);

    List<Album> findAllByPhotographId(UUID photographId);

    List<Album> findAllByClientId(UUID clientId);

    List<Album> findByTtdBeforeAndTtdIsNotNull(Instant now);

    boolean existsByName(String name);

    void delete(Album album);

    List<Album> findAll();

    List<Album> findAllPublic();

    Optional<Album> findPublicByName(String name);
}
