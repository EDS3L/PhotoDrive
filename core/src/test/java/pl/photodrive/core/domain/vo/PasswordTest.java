package pl.photodrive.core.domain.vo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class PasswordTest {

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowWhenNullOrEmpty(String value) {
        assertThatThrownBy(() -> new Password(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
    }

    @Test
    void shouldThrowWhenTooShort() {
        assertThatThrownBy(() -> new Password("Abc1!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 8 characters");
    }

    @Test
    void shouldThrowWhenNoUppercaseLetter() {
        assertThatThrownBy(() -> new Password("abcdefg1!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uppercase");
    }

    @Test
    void shouldThrowWhenNoLowercaseLetter() {
        assertThatThrownBy(() -> new Password("ABCDEFG1!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lowercase");
    }

    @Test
    void shouldThrowWhenNoDigit() {
        assertThatThrownBy(() -> new Password("Abcdefgh!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("digit");
    }

    @Test
    void shouldThrowWhenNoSpecialCharacter() {
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
    void shouldAcceptValidPasswords(String value) {
        assertThatCode(() -> new Password(value)).doesNotThrowAnyException();
    }

    @Test
    void shouldReturnValueOnValidPassword() {
        Password password = new Password("Valid1!A");
        assertThat(password.value()).isEqualTo("Valid1!A");
    }

    @Test
    void shouldBeEqualWhenSameValue() {
        assertThat(new Password("Valid1!A")).isEqualTo(new Password("Valid1!A"));
    }
}
