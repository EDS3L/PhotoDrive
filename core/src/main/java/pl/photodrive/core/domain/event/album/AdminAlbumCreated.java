package pl.photodrive.core.domain.event.album;

import pl.photodrive.core.domain.model.User;

public record AdminAlbumCreated(String albumName, User admin) {
}
