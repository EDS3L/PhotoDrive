package pl.photodrive.core.domain.model;

import org.junit.jupiter.api.Test;
import pl.photodrive.core.application.port.password.PasswordHasher;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.Password;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserTest {

    private final Password dummyPassword = new Password("SecurePass123!");

    @Test
    void shouldCreateUserWithCorrectInitialState() {
        User user = User.create("Jan", new Email("jan@test.pl"), dummyPassword, Role.PHOTOGRAPHER, "raw");

        assertNotNull(user.getId());
        assertTrue(user.isActive());
        assertTrue(user.isChangePasswordOnNextLogin());
        assertTrue(user.getRoles().contains(Role.PHOTOGRAPHER));
    }

    @Test
    void shouldAddRoleCorrectly() {
        User user = User.create("Jan", new Email("jan@test.pl"), dummyPassword, Role.PHOTOGRAPHER, "raw");
        user.addRole(Role.ADMIN);

        assertTrue(user.getRoles().contains(Role.ADMIN));
        assertEquals(2, user.getRoles().size());
    }

    @Test
    void shouldVerifyPasswordSuccessfully() {
        PasswordHasher hasher = mock(PasswordHasher.class);
        when(hasher.matches("raw", dummyPassword.value())).thenReturn(true);
        
        User user = User.create("Jan", new Email("jan@test.pl"), dummyPassword, Role.PHOTOGRAPHER, "raw");

        assertDoesNotThrow(() -> user.verifyPassword("raw", hasher));
    }
}
