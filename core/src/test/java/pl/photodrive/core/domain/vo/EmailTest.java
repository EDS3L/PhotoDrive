package pl.photodrive.core.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pl.photodrive.core.domain.exception.EmailException;

import static org.assertj.core.api.Assertions.*;

class EmailTest {

    @Test
    @DisplayName("Email wraps a well-formed address")
    void shouldCreateEmailWithValidValue() {
        // When
        Email email = new Email("user@example.com");

        // Then
        assertThat(email.value()).isEqualTo("user@example.com");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Email cannot be null or empty")
    void shouldThrowWhenNullOrEmpty(String value) {
        // When / Then
        assertThatThrownBy(() -> new Email(value))
                .isInstanceOf(EmailException.class)
                .hasMessageContaining("empty or null");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "notanemail",
            "@nodomain.com",
            "missing@",
            "missing@domain",
            "spaces in@email.com",
            "double@@at.com",
            "nodot@domain"
    })
    @DisplayName("Malformed address is rejected at construction, not later")
    void shouldThrowWhenFormatInvalid(String value) {
        // When / Then
        assertThatThrownBy(() -> new Email(value))
                .isInstanceOf(EmailException.class)
                .hasMessageContaining("Invalid email format");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "user@domain.com",
            "user.name+tag@sub.domain.org",
            "a@b.pl",
            "USER@DOMAIN.COM",
            "user123@example.co"
    })
    @DisplayName("Common valid address formats are accepted")
    void shouldAcceptValidEmailFormats(String value) {
        // When / Then
        assertThatCode(() -> new Email(value)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Two emails with the same address are equal")
    void shouldBeEqualWhenSameValue() {
        // When / Then
        assertThat(new Email("user@example.com")).isEqualTo(new Email("user@example.com"));
    }

    @Test
    @DisplayName("Emails with different addresses are not equal")
    void shouldNotBeEqualWhenDifferentValues() {
        // When / Then
        assertThat(new Email("a@example.com")).isNotEqualTo(new Email("b@example.com"));
    }

    @Test
    @DisplayName("Equal emails share a hash code, so they work as map keys")
    void shouldHaveSameHashCodeForEqualEmails() {
        // When / Then
        assertThat(new Email("user@example.com").hashCode())
                .isEqualTo(new Email("user@example.com").hashCode());
    }
}
