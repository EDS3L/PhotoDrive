package pl.photodrive.core.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pl.photodrive.core.domain.exception.StudyProgressException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudySyncCodeTest {

    private static final String CODE = "k7Qp2xWmZ-a_9LrT0bNc4e";

    @Test
    @DisplayName("A 22-character base64url code (128 random bits) is accepted as a sync code")
    void shouldAcceptWellFormedCode() {
        // When
        StudySyncCode code = new StudySyncCode(CODE);

        // Then
        assertThat(code.value()).isEqualTo(CODE);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "short",
            "k7Qp2xWmZ-a_9LrT0bNc4",
            "k7Qp2xWmZ-a_9LrT0bNc4ee",
            "k7Qp2xWmZ+a/9LrT0bNc4e",
            "../../etc/passwd000000"
    })
    @DisplayName("A code that is not exactly 22 base64url characters is rejected, so a short guessable key can never be used")
    void shouldRejectMalformedCode(String value) {
        // When / Then
        assertThatThrownBy(() -> new StudySyncCode(value))
                .isInstanceOf(StudyProgressException.class);
    }

    @Test
    @DisplayName("The stored key is a SHA-256 hash of the code, so a database leak does not reveal the codes that open anyone's progress")
    void shouldHashCodeForStorage() {
        // When
        String hashed = new StudySyncCode(CODE).hashed();

        // Then
        assertThat(hashed).hasSize(64).matches("[0-9a-f]{64}").doesNotContain(CODE);
        assertThat(hashed).isEqualTo(new StudySyncCode(CODE).hashed());
        assertThat(hashed).isNotEqualTo(new StudySyncCode("k7Qp2xWmZ-a_9LrT0bNc4f").hashed());
    }

    @Test
    @DisplayName("The code never shows up in logs through toString, because it is the only key to the progress")
    void shouldMaskCodeInToString() {
        // When / Then
        assertThat(new StudySyncCode(CODE).toString()).doesNotContain(CODE);
    }
}
