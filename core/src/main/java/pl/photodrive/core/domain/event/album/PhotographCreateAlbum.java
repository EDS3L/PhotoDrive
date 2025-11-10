package pl.photodrive.core.domain.event.album;

import pl.photodrive.core.domain.model.User;

public record PhotographCreateAlbum(User photograph, String name) {
}
