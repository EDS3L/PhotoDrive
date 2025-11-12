package pl.photodrive.core.domain.event.album;

import pl.photodrive.core.domain.vo.FileName;

import java.io.InputStream;

public record FileAddedToAlbum(FileName fileName, String albumName, InputStream fileData) {
}
