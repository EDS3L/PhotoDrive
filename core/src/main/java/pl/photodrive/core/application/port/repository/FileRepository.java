package pl.photodrive.core.application.port.repository;


import pl.photodrive.core.domain.model.File;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.domain.vo.FileName;

import java.util.Optional;

public interface FileRepository {

    boolean existsByAlbumIdAndFileName(AlbumId albumId, FileName fileName);
    Optional<File> findById(FileId fileId);
    File save(File file);

}
