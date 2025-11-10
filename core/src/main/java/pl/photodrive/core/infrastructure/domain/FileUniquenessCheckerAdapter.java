package pl.photodrive.core.infrastructure.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.photodrive.core.domain.port.FileUniquenessChecker;
import pl.photodrive.core.domain.port.repository.FileRepository;
import pl.photodrive.core.domain.vo.FileName;
@Component
@RequiredArgsConstructor
public class FileUniquenessCheckerAdapter implements FileUniquenessChecker {

    private final FileRepository fileRepository;

    @Override
    public boolean isFileNameTaken(FileName fileName) {
        return fileRepository.existsByFileName(fileName);
    }
}
