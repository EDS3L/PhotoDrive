package pl.photodrive.core.application.command.album;

import java.util.UUID;

public record GetPhotoPathCommand(UUID albumId, String fileName, Integer width, Integer height) {
}
