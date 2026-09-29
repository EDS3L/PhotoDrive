package pl.photodrive.core.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.photodrive.core.domain.exception.StudyProgressException;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudyProgressTest {

    @Test
    @DisplayName("Learned questions are stored sorted and without duplicates, so the same progress always has one representation")
    void shouldNormaliseLearnedQuestions() {
        // When
        List<Integer> learned = StudyProgress.validLearned(List.of(9, 1, 9, 67));

        // Then
        assertThat(learned).containsExactly(1, 9, 67);
    }

    @Test
    @DisplayName("An empty list is valid progress, so unmarking the last question also syncs")
    void shouldAcceptEmptyProgress() {
        // When / Then
        assertThat(StudyProgress.validLearned(List.of())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, StudyProgress.MAX_QUESTION_NUMBER + 1})
    @DisplayName("A question number outside 1..500 is rejected, which also caps how much one code can store")
    void shouldRejectQuestionNumberOutOfRange(int number) {
        // When / Then
        assertThatThrownBy(() -> StudyProgress.validLearned(List.of(1, number)))
                .isInstanceOf(StudyProgressException.class);
    }

    @Test
    @DisplayName("The highest allowed question number is still accepted")
    void shouldAcceptUpperBound() {
        // When / Then
        assertThat(StudyProgress.validLearned(List.of(StudyProgress.MAX_QUESTION_NUMBER)))
                .containsExactly(StudyProgress.MAX_QUESTION_NUMBER);
    }

    @Test
    @DisplayName("A missing list or an empty entry is rejected as a bad request instead of failing later")
    void shouldRejectMissingOrNullEntries() {
        // When / Then
        assertThatThrownBy(() -> StudyProgress.validLearned(null)).isInstanceOf(StudyProgressException.class);
        assertThatThrownBy(() -> StudyProgress.validLearned(Arrays.asList(1, null)))
                .isInstanceOf(StudyProgressException.class);
    }

    @Test
    @DisplayName("Progress that was never saved is empty and has no timestamp, so the client knows to upload its own")
    void shouldDescribeNeverSavedProgress() {
        // When
        StudyProgress empty = StudyProgress.empty();

        // Then
        assertThat(empty.learned()).isEmpty();
        assertThat(empty.updatedAt()).isNull();
    }
}
