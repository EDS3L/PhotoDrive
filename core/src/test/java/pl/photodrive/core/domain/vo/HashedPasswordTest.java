package pl.photodrive.core.domain.vo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class HashedPasswordTest {

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowWhenNullOrEmpty(String value) {
        assertThatThrownBy(() -> new HashedPassword(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"   ", "\t", "\n"})
    void shouldThrowWhenBlankOnly(String value) {
        assertThatThrownBy(() -> new HashedPassword(value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCreateWithValidHash() {
        HashedPassword hp = new HashedPassword("$2a$12$hashedvalue");
        assertThat(hp.value()).isEqualTo("$2a$12$hashedvalue");
    }

    @Test
    void shouldBeEqualWhenSameValue() {
        assertThat(new HashedPassword("hash123")).isEqualTo(new HashedPassword("hash123"));
    }

    @Test
    void shouldNotBeEqualWhenDifferentValues() {
        assertThat(new HashedPassword("hash123")).isNotEqualTo(new HashedPassword("hash456"));
    }

    @Test
    void shouldHaveSameHashCodeForEqualObjects() {
        assertThat(new HashedPassword("hash123").hashCode())
                .isEqualTo(new HashedPassword("hash123").hashCode());
    }
}
