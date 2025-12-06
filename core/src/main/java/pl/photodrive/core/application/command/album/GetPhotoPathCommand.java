package pl.photodrive.core.application.command.album;


import jakarta.servlet.ServletContext;

import java.nio.file.Path;
import java.util.UUID;

public record GetPhotoPathCommand(UUID albumId, String fileName, Path fileStorageLocation, ServletContext servletContext, Integer width, Integer height) {
}
