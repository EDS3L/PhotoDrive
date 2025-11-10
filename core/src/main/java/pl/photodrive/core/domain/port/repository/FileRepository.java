package pl.photodrive.core.domain.port.repository;


import pl.photodrive.core.domain.vo.FileName;

public interface FileRepository {

    boolean existsByFileName(FileName fileName);

}
