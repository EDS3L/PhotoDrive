package pl.photodrive.core.infrastructure.jpa.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.photodrive.core.domain.model.StudyProgress;
import pl.photodrive.core.domain.vo.StudySyncCode;
import pl.photodrive.core.infrastructure.jpa.entity.StudyProgressEntity;
import pl.photodrive.core.infrastructure.jpa.repository.StudyProgressJpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class StudyProgressStoreAdapterTest {

    private static final StudySyncCode CODE = new StudySyncCode("k7Qp2xWmZ-a_9LrT0bNc4e");

    @Mock
    private StudyProgressJpaRepository jpa;

    @InjectMocks
    private StudyProgressStoreAdapter adapter;

    @Test
    @DisplayName("The row is keyed by the hash of the code, never by the code itself")
    void shouldKeyRowByHashedCode() {
        // When
        adapter.save(CODE, List.of(1, 9, 67));

        // Then
        ArgumentCaptor<StudyProgressEntity> saved = ArgumentCaptor.forClass(StudyProgressEntity.class);
        then(jpa).should().save(saved.capture());
        assertThat(saved.getValue().getCodeHash()).isEqualTo(CODE.hashed()).isNotEqualTo(CODE.value());
        assertThat(saved.getValue().getLearned()).isEqualTo("1,9,67");
        assertThat(saved.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("A saved row reads back as the same list of questions, including an empty one")
    void shouldReadBackStoredList() {
        // Given
        Instant at = Instant.ofEpochMilli(1234);
        given(jpa.findById(CODE.hashed())).willReturn(Optional.of(new StudyProgressEntity(CODE.hashed(), "1,9,67", at)));

        // When / Then
        assertThat(adapter.find(CODE)).contains(new StudyProgress(List.of(1, 9, 67), at));

        // Given - the last question was unmarked
        given(jpa.findById(CODE.hashed())).willReturn(Optional.of(new StudyProgressEntity(CODE.hashed(), "", at)));

        // When / Then
        assertThat(adapter.find(CODE)).contains(new StudyProgress(List.of(), at));
    }
}
