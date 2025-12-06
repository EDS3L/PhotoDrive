package pl.photodrive.core.domain.event.album;

import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.domain.vo.FileName;

// test
public record FileAddedToClientAlbum(FileId fileId, FileName fileName, String albumName, String photographEmail) {
}
