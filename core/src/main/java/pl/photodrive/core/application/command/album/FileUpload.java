package pl.photodrive.core.application.command.album;

import lombok.extern.slf4j.Slf4j;
import pl.photodrive.core.domain.vo.FileName;

@Slf4j
public record FileUpload(FileName fileName, long sizeBytes, String contentType, String tempId) {

}