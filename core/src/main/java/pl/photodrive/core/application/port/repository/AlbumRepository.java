package pl.photodrive.core.application.port.repository;

import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.vo.AlbumId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlbumRepository {
    Album save(Album album);

    Optional<Album> findByAlbumId(AlbumId albumId);

    Album findByPhotographId(UUID photographId);

    Optional<Album> findByName(String name);

    List<Album> findByClientId(UUID clientId);

    boolean existsByName(String name);

    void delete(Album album);

    List<Album> findAll();
}
