package pl.photodrive.core.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.photodrive.core.application.port.study.StudyProgressStorePort;
import pl.photodrive.core.domain.model.StudyProgress;
import pl.photodrive.core.domain.vo.StudySyncCode;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyProgressService {

    private final StudyProgressStorePort store;

    @Transactional(readOnly = true)
    public StudyProgress get(String code) {
        return store.find(new StudySyncCode(code)).orElseGet(StudyProgress::empty);
    }

    @Transactional
    public StudyProgress save(String code, List<Integer> learned) {
        StudySyncCode syncCode = new StudySyncCode(code);
        return store.save(syncCode, StudyProgress.validLearned(learned));
    }
}
