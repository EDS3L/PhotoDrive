package pl.photodrive.core.domain.event.album;

import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.domain.vo.FileName;

public record FileRenamedInAlbum(FileId fileId, FileName oldFileName, FileName newFileName, String albumName) {
}
