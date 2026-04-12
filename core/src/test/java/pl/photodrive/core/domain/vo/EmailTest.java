package pl.photodrive.core.domain.vo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pl.photodrive.core.domain.exception.EmailException;

import static org.assertj.core.api.Assertions.*;

class EmailTest {

    @Test
    void shouldCreateEmailWithValidValue() {
        // When
        Email email = new Email("user@example.com");

        // Then
        assertThat(email.value()).isEqualTo("user@example.com");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowWhenNullOrEmpty(String value) {
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
    void shouldThrowWhenFormatInvalid(String value) {
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
    void shouldAcceptValidEmailFormats(String value) {
        assertThatCode(() -> new Email(value)).doesNotThrowAnyException();
    }

    @Test
    void shouldBeEqualWhenSameValue() {
        assertThat(new Email("user@example.com")).isEqualTo(new Email("user@example.com"));
    }

    @Test
    void shouldNotBeEqualWhenDifferentValues() {
        assertThat(new Email("a@example.com")).isNotEqualTo(new Email("b@example.com"));
    }

    @Test
    void shouldHaveSameHashCodeForEqualEmails() {
        assertThat(new Email("user@example.com").hashCode())
                .isEqualTo(new Email("user@example.com").hashCode());
    }
}
