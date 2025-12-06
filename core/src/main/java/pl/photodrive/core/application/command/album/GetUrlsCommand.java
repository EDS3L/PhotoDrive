package pl.photodrive.core.application.command.album;

import java.util.UUID;

public record GetUrlsCommand(UUID albumId, String domainPath, Integer width, Integer height) {
}
