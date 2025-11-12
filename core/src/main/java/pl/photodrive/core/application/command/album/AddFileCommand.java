package pl.photodrive.core.application.command.album;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record AddFileCommand(List<MultipartFile> multipartFiles, String albumName) {
}
