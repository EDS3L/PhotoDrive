package pl.photodrive.core.domain.event.album;

import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.domain.vo.FileName;

import java.io.InputStream;

public record FileAddedToAlbum(FileId fileId, FileName fileName, String albumName) {
}
