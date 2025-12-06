package pl.photodrive.core.application.command.file;

import java.util.List;
import java.util.UUID;

public record RemoveFileCommand(List<UUID> fileIdList, UUID albumId) {
}
