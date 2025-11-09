package pl.photodrive.core.domain.event;

import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.UserId;

public record PhotographerRootAlbumCreated(AlbumId albumId, UserId photographerId, Email photographerEmail) {
}
