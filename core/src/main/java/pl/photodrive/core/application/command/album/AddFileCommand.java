package pl.photodrive.core.application.command.album;

import org.springframework.web.multipart.MultipartFile;

public record AddFileCommand(MultipartFile multipartFile) {
}
