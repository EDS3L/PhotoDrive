package pl.photodrive.core.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.photodrive.core.application.port.study.StudyProgressStorePort;
import pl.photodrive.core.domain.exception.StudyProgressException;
import pl.photodrive.core.domain.model.StudyProgress;
import pl.photodrive.core.domain.vo.StudySyncCode;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class StudyProgressServiceTest {

    private static final String CODE = "k7Qp2xWmZ-a_9LrT0bNc4e";

    @Mock
    private StudyProgressStorePort store;

    @InjectMocks
    private StudyProgressService service;

    @Test
    @DisplayName("Reading a code that was never saved returns empty progress, not an error, so a fresh device can join and upload")
    void shouldReturnEmptyProgressForUnknownCode() {
        // Given
        given(store.find(new StudySyncCode(CODE))).willReturn(Optional.empty());

        // When
        StudyProgress progress = service.get(CODE);

        // Then
        assertThat(progress.learned()).isEmpty();
        assertThat(progress.updatedAt()).isNull();
    }

    @Test
    @DisplayName("Reading a saved code returns the stored progress with its timestamp")
    void shouldReturnStoredProgress() {
        // Given
        StudyProgress stored = new StudyProgress(List.of(1, 9), Instant.ofEpochMilli(1234));
        given(store.find(new StudySyncCode(CODE))).willReturn(Optional.of(stored));

        // When / Then
        assertThat(service.get(CODE)).isEqualTo(stored);
    }

    @Test
    @DisplayName("Saving stores the normalised list, so duplicates sent by a client never reach the database")
    void shouldSaveNormalisedProgress() {
        // Given
        StudyProgress saved = new StudyProgress(List.of(1, 9), Instant.ofEpochMilli(1234));
        given(store.save(new StudySyncCode(CODE), List.of(1, 9))).willReturn(saved);

        // When
        StudyProgress result = service.save(CODE, List.of(9, 1, 9));

        // Then
        assertThat(result).isEqualTo(saved);
        then(store).should().save(new StudySyncCode(CODE), List.of(1, 9));
    }

    @Test
    @DisplayName("A malformed code is refused before touching storage, on reads and on writes")
    void shouldRejectMalformedCodeWithoutTouchingStorage() {
        // When / Then
        assertThrows(StudyProgressException.class, () -> service.get("short"));
        assertThrows(StudyProgressException.class, () -> service.save("short", List.of(1)));
        then(store).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Invalid question numbers are refused before anything is written")
    void shouldRejectInvalidProgressWithoutWriting() {
        // When / Then
        assertThrows(StudyProgressException.class, () -> service.save(CODE, List.of(0)));
        then(store).should(never()).save(any(), any());
    }
}
