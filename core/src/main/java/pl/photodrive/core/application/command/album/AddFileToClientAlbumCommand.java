package pl.photodrive.core.application.command.album;

import org.springframework.web.multipart.MultipartFile;

public record AddFileToClientAlbumCommand(MultipartFile[] multipartFiles, String albumName) {
}
