package pl.photodrive.core.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class HashedPasswordTest {

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Hashed password cannot be null or empty")
    void shouldThrowWhenNullOrEmpty(String value) {
        // When / Then
        assertThatThrownBy(() -> new HashedPassword(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Hashed password cannot be whitespace only")
    void shouldThrowWhenBlankOnly(String value) {
        // When / Then
        assertThatThrownBy(() -> new HashedPassword(value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Hashed password wraps a BCrypt hash")
    void shouldCreateWithValidHash() {
        // When
        HashedPassword hp = new HashedPassword("$2a$12$hashedvalue");

        // Then
        assertThat(hp.value()).isEqualTo("$2a$12$hashedvalue");
    }

    @Test
    @DisplayName("Two hashed passwords with the same hash are equal")
    void shouldBeEqualWhenSameValue() {
        // When / Then
        assertThat(new HashedPassword("hash123")).isEqualTo(new HashedPassword("hash123"));
    }

    @Test
    @DisplayName("Different hashes are not equal")
    void shouldNotBeEqualWhenDifferentValues() {
        // When / Then
        assertThat(new HashedPassword("hash123")).isNotEqualTo(new HashedPassword("hash456"));
    }

    @Test
    @DisplayName("Equal hashed passwords share a hash code")
    void shouldHaveSameHashCodeForEqualObjects() {
        // When / Then
        assertThat(new HashedPassword("hash123").hashCode())
                .isEqualTo(new HashedPassword("hash123").hashCode());
    }
}
