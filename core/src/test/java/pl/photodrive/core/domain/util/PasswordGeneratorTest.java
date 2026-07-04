package pl.photodrive.core.domain.util;

import org.junit.jupiter.api.Test;
import pl.photodrive.core.domain.vo.Password;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PasswordGeneratorTest {

    @Test
    void shouldGeneratePasswordThatSatisfiesPasswordRules() {
        // Given / When — wielokrotnie, bo generator jest losowy
        for (int i = 0; i < 500; i++) {
            String generated = PasswordGenerator.generate();

            // Then — musi przejść walidację Password VO (upper/lower/digit/special, min 8)
            assertThatCode(() -> new Password(generated)).doesNotThrowAnyException();
            assertThat(generated).hasSize(16);
        }
    }

    @Test
    void shouldGenerateDifferentPasswordsEachTime() {
        String first = PasswordGenerator.generate();
        String second = PasswordGenerator.generate();

        assertThat(first).isNotEqualTo(second);
    }
}
