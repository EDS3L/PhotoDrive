package pl.photodrive.core.application.command.album;

import pl.photodrive.core.domain.vo.AlbumId;

public record RemoveAlbumPhotographCommand(AlbumId albumId) {
}
