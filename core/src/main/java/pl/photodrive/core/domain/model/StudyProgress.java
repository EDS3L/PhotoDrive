package pl.photodrive.core.domain.model;

import pl.photodrive.core.domain.exception.StudyProgressException;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public record StudyProgress(List<Integer> learned, Instant updatedAt) {

    public static final int MAX_QUESTION_NUMBER = 500;

    public StudyProgress {
        learned = List.copyOf(learned);
    }

    public static StudyProgress empty() {
        return new StudyProgress(List.of(), null);
    }

    public static List<Integer> validLearned(Collection<Integer> learned) {
        if (learned == null) {
            throw new StudyProgressException("Learned questions are required");
        }
        if (learned.stream().anyMatch(Objects::isNull)) {
            throw new StudyProgressException("Question number cannot be empty");
        }
        if (learned.stream().anyMatch(n -> n < 1 || n > MAX_QUESTION_NUMBER)) {
            throw new StudyProgressException("Question number must be between 1 and " + MAX_QUESTION_NUMBER);
        }
        return learned.stream().distinct().sorted().toList();
    }
}
