package pl.photodrive.core.domain.port;

import pl.photodrive.core.domain.model.Album;

public interface AlbumSaver {

    Album saveAlbum(Album album);
}
