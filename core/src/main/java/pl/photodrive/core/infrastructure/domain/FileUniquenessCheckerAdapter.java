package pl.photodrive.core.infrastructure.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.photodrive.core.application.port.file.FileUniquenessChecker;
import pl.photodrive.core.application.port.repository.FileRepository;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.FileName;

@Component
@RequiredArgsConstructor
public class FileUniquenessCheckerAdapter implements FileUniquenessChecker {

    private final FileRepository fileRepository;

    @Override
    public boolean isFileNameTaken(AlbumId albumId, FileName fileName) {
        return fileRepository.existsByAlbumIdAndFileName(albumId, fileName);
    }
}
