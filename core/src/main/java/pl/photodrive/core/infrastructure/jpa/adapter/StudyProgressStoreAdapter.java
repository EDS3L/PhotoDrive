package pl.photodrive.core.infrastructure.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.photodrive.core.application.port.study.StudyProgressStorePort;
import pl.photodrive.core.domain.model.StudyProgress;
import pl.photodrive.core.domain.vo.StudySyncCode;
import pl.photodrive.core.infrastructure.jpa.entity.StudyProgressEntity;
import pl.photodrive.core.infrastructure.jpa.repository.StudyProgressJpaRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class StudyProgressStoreAdapter implements StudyProgressStorePort {

    private final StudyProgressJpaRepository jpa;

    @Override
    public Optional<StudyProgress> find(StudySyncCode code) {
        return jpa.findById(code.hashed())
                .map(entity -> new StudyProgress(parse(entity.getLearned()), entity.getUpdatedAt()));
    }

    @Override
    public StudyProgress save(StudySyncCode code, List<Integer> learned) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        jpa.save(StudyProgressEntity.builder()
                .codeHash(code.hashed())
                .learned(learned.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .updatedAt(now)
                .build());
        return new StudyProgress(learned, now);
    }

    private static List<Integer> parse(String learned) {
        if (learned.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(learned.split(",")).map(Integer::valueOf).toList();
    }
}
