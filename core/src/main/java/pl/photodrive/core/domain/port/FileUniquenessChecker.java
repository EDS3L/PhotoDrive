package pl.photodrive.core.domain.port;

import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.FileName;

public interface FileUniquenessChecker {

    boolean isFileNameTaken(AlbumId albumId, FileName fileName);
}
