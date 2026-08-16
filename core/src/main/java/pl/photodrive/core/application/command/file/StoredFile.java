package pl.photodrive.core.application.command.file;

import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.domain.vo.FileName;

public record StoredFile(FileId fileId, FileName fileName) {
}
