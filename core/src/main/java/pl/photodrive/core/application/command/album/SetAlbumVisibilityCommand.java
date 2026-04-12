package pl.photodrive.core.application.command.album;

import java.util.UUID;

public record SetAlbumVisibilityCommand(UUID albumId, boolean isPublic) {
}
