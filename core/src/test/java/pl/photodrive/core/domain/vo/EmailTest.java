package pl.photodrive.core.domain.vo;

import org.junit.jupiter.api.Test;
import pl.photodrive.core.domain.exception.EmailException;

import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    @Test
    void shouldCreateValidEmail() {
        assertDoesNotThrow(() -> new Email("test@photodrive.pl"));
        assertDoesNotThrow(() -> new Email("user.name+tag@gmail.com"));
    }

    @Test
    void shouldThrowExceptionForInvalidEmail() {
        assertThrows(EmailException.class, () -> new Email("invalid-email"));
        assertThrows(EmailException.class, () -> new Email("@no-user.com"));
        assertThrows(EmailException.class, () -> new Email("user@.com"));
    }
}
