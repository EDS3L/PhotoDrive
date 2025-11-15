package pl.photodrive.core.domain.port.repository;


import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.FileName;

public interface FileRepository {

    boolean existsByAlbumIdAndFileName(AlbumId albumId, FileName fileName);

}
