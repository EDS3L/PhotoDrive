package pl.photodrive.core.application.port.file;

import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.FileName;

public interface FileUniquenessChecker {

    boolean isFileNameTaken(AlbumId albumId, FileName fileName);
}
