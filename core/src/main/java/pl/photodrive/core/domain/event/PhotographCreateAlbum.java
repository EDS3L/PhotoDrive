package pl.photodrive.core.domain.event;

import pl.photodrive.core.domain.model.User;

public record PhotographCreateAlbum(User photograph, String name) {
}
