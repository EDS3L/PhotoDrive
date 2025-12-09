package pl.photodrive.core.domain.event.album;

import pl.photodrive.core.domain.vo.AlbumPath;
import pl.photodrive.core.domain.vo.FileName;

public record FileSwaped(AlbumPath albumPath, AlbumPath targetAlbumPath, FileName fileName) {
}
