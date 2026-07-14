package pl.photodrive.core.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class PasswordTest {

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Password cannot be null or empty")
    void shouldThrowWhenNullOrEmpty(String value) {
        // When / Then
        assertThatThrownBy(() -> new Password(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
    }

    @Test
    @DisplayName("Password shorter than the minimum length is rejected")
    void shouldThrowWhenTooShort() {
        // When / Then
        assertThatThrownBy(() -> new Password("Abc1!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 8 characters");
    }

    @Test
    @DisplayName("Password must contain an upper-case letter")
    void shouldThrowWhenNoUppercaseLetter() {
        // When / Then
        assertThatThrownBy(() -> new Password("abcdefg1!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uppercase");
    }

    @Test
    @DisplayName("Password must contain a lower-case letter")
    void shouldThrowWhenNoLowercaseLetter() {
        // When / Then
        assertThatThrownBy(() -> new Password("ABCDEFG1!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lowercase");
    }

    @Test
    @DisplayName("Password must contain a digit")
    void shouldThrowWhenNoDigit() {
        // When / Then
        assertThatThrownBy(() -> new Password("Abcdefgh!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("digit");
    }

    @Test
    @DisplayName("Password must contain a special character")
    void shouldThrowWhenNoSpecialCharacter() {
        // When / Then
        assertThatThrownBy(() -> new Password("Abcdefg1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("special character");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Valid1!A",
            "SecurePass9@",
            "My$tr0ngP@ss",
            "Test1234!"
    })
    @DisplayName("Password satisfying every rule is accepted")
    void shouldAcceptValidPasswords(String value) {
        // When / Then
        assertThatCode(() -> new Password(value)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Password exposes its raw value only after validation")
    void shouldReturnValueOnValidPassword() {
        // When
        Password password = new Password("Valid1!A");

        // Then
        assertThat(password.value()).isEqualTo("Valid1!A");
    }

    @Test
    @DisplayName("Two passwords with the same value are equal")
    void shouldBeEqualWhenSameValue() {
        // When / Then
        assertThat(new Password("Valid1!A")).isEqualTo(new Password("Valid1!A"));
    }
}
