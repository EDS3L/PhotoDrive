package pl.photodrive.core.application.port.study;

import pl.photodrive.core.domain.model.StudyProgress;
import pl.photodrive.core.domain.vo.StudySyncCode;

import java.util.List;
import java.util.Optional;

public interface StudyProgressStorePort {

    Optional<StudyProgress> find(StudySyncCode code);

    StudyProgress save(StudySyncCode code, List<Integer> learned);
}
