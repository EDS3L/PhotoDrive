package pl.photodrive.core.presentation.dto.study;

import java.util.List;

public record StudyProgressDto(List<Integer> learned, Long updatedAt) {
}
