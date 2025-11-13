package pl.photodrive.core.application.command.album;


import java.util.UUID;

public record CreateAlbumCommand(String albumName, UUID clientId) {
}
