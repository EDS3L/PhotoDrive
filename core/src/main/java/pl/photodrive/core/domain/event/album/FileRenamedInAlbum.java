package pl.photodrive.core.domain.event.album;


import pl.photodrive.core.domain.vo.FileName;

public record FileRenamedInAlbum(String path, FileName oldFileName, FileName newFileName) {
}
