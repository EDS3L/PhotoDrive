package pl.photodrive.core.infrastructure.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.port.AlbumSaver;
import pl.photodrive.core.domain.port.repository.AlbumRepository;
@Component
@RequiredArgsConstructor
public class AlbumSaverAdapter implements AlbumSaver {

    private final AlbumRepository albumRepository;

    @Override
    public Album saveAlbum(Album album) {
        return albumRepository.save(album);
    }
}
