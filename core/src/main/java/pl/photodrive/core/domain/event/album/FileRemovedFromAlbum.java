package pl.photodrive.core.domain.event.album;

import pl.photodrive.core.domain.vo.FileId;

public record FileRemovedFromAlbum(FileId fileId, String albumName) {
}
