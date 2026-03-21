package pl.photodrive.core.domain.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordTest {
    @Test
    void shouldCreateValidPassword() {
        assertDoesNotThrow(() -> new Password("SecurePass123!"));
    }

    @Test
    void shouldThrowWhenTooShort() {
        assertThrows(IllegalArgumentException.class, () -> new Password("Ab1!"));
    }

    @Test
    void shouldThrowWhenNoUppercase() {
        assertThrows(IllegalArgumentException.class, () -> new Password("securepass123!"));
    }

    @Test
    void shouldThrowWhenNoSpecialChar() {
        assertThrows(IllegalArgumentException.class, () -> new Password("SecurePass123"));
    }
}