package pl.photodrive.core.application.command.album;

import java.util.List;
import java.util.UUID;

public record ChangeWatermarkCommand(UUID albumId, List<UUID> filesUUIDList, boolean hasWatermark) {
}
