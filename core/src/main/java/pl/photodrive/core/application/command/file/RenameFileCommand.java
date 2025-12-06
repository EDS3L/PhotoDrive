package pl.photodrive.core.application.command.file;

import java.util.UUID;

public record RenameFileCommand(UUID albumId, UUID fileId, String newFileName) {
}
