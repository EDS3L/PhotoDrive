package pl.photodrive.core.domain.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.photodrive.core.domain.vo.Password;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PasswordGeneratorTest {

    @Test
    @DisplayName("Generated onboarding password satisfies every password rule")
    void shouldGeneratePasswordThatSatisfiesPasswordRules() {
        // Given / When - repeated, because the generator is random
        for (int i = 0; i < 500; i++) {
            String generated = PasswordGenerator.generate();

            // Then - it must pass Password VO validation (upper, lower, digit, special, min 8)
            assertThatCode(() -> new Password(generated)).doesNotThrowAnyException();
            assertThat(generated).hasSize(16);
        }
    }

    @Test
    @DisplayName("Each onboarding password is different, so accounts do not share a start password")
    void shouldGenerateDifferentPasswordsEachTime() {
        // Given
        String first = PasswordGenerator.generate();

        // When
        String second = PasswordGenerator.generate();

        // Then
        assertThat(first).isNotEqualTo(second);
    }
}
