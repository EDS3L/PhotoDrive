package pl.photodrive.core.application.command.album;

import java.util.UUID;

public record SwapFileCommand(UUID albumId, UUID targetAlbumId, UUID fileId) {
}
