package pl.photodrive.core.domain.port.repository;

import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.Email;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlbumRepository {
    Album save(Album album);
    Optional<Album> findByAlbumId(AlbumId albumId);
    List<Album> findByPhotographId(UUID photographerId);
    List<Album> findByClientId(UUID clientId);
    boolean existsByName(String name);

}
