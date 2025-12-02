package pl.photodrive.core.domain.event.album;

import pl.photodrive.core.domain.vo.AlbumPath;

public record ExpiredAlbumRemoved(AlbumPath path) {
}
