package pl.photodrive.core.application.command.album;

import java.util.UUID;

public record ChangeDisplaySettingsCommand(UUID albumId, String displayName, int displayOrder) {
}
