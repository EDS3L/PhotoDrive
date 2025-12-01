package pl.photodrive.core.application.command.album;

import java.time.Instant;
import java.util.UUID;

public record SetTTDCommand(UUID albumId, Instant ttd) {
}
