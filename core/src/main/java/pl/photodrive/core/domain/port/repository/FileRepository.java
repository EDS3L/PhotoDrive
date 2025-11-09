package pl.photodrive.core.domain.port.repository;

import pl.photodrive.core.domain.model.File;
import pl.photodrive.core.domain.vo.FileName;

import java.util.Optional;

public interface FileRepository {

    File save(File file);
    Optional<File> findByFileName(FileName fileName);

}
