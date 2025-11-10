package pl.photodrive.core.domain.event.album;

import pl.photodrive.core.domain.vo.FileName;

import java.io.InputStream;

public record FileAddedToClientAlbum(FileName fileName, String albumName, String photographEmail, InputStream fileData) {
}
