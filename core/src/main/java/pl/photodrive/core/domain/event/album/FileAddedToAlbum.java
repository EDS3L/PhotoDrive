package pl.photodrive.core.domain.event.album;

import pl.photodrive.core.domain.vo.FileName;

public record FileAddedToAlbum(FileName fileName, String photographEmail) {
}
