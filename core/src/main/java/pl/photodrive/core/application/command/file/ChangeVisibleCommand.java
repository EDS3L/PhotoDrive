package pl.photodrive.core.application.command.file;

import java.util.List;
import java.util.UUID;

public record ChangeVisibleCommand(UUID albumId, List<UUID> fileIds, boolean isVisible ) {
}
