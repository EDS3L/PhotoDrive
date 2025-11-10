package pl.photodrive.core.domain.port;

import pl.photodrive.core.domain.vo.FileName;

public interface FileUniquenessChecker {

    boolean isFileNameTaken(FileName fileName);
}
