package pl.photodrive.core.application.command.album;

import java.util.UUID;

public record CreateAlbumForPhotographer(String email, UUID photographId) {
}
