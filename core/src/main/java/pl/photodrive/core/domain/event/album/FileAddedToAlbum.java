package pl.photodrive.core.domain.event.album;

import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.domain.vo.FileName;

public record FileAddedToAlbum(FileId fileId, FileName fileName, String albumName) {
}
