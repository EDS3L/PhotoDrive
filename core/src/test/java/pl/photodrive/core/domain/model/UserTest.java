package pl.photodrive.core.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.photodrive.core.domain.service.PasswordHasher;
import pl.photodrive.core.domain.exception.UserException;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.HashedPassword;
import pl.photodrive.core.domain.vo.UserId;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private final PasswordHasher passwordHasher = new PasswordHasher() {
        @Override
        public String encode(CharSequence raw) {
            return "hashed_" + raw;
        }

        @Override
        public boolean matches(CharSequence raw, String hashed) {
            return hashed.equals("hashed_" + raw);
        }
    };

    private static final String RAW_PASSWORD = "Test1234!";
    private static final HashedPassword HASHED_PASSWORD = new HashedPassword("Test1234!");

    private User admin;
    private User photographer;
    private User client;

    @BeforeEach
    void setUp() {
        admin = User.create("Admin", new Email("admin@photodrive.pl"), HASHED_PASSWORD, Role.ADMIN);
        photographer = User.create("Photographer", new Email("photographer@photodrive.pl"), HASHED_PASSWORD, Role.PHOTOGRAPHER);
        client = User.create("Client", new Email("client@photodrive.pl"), HASHED_PASSWORD, Role.CLIENT);
    }

    @Test
    void shouldCreateUserWithCorrectData() {
        // When
        User user = User.create("Jan", new Email("jan@photodrive.pl"), HASHED_PASSWORD, Role.PHOTOGRAPHER);

        // Then
        assertNotNull(user.getId());
        assertEquals("Jan", user.getName());
        assertEquals("jan@photodrive.pl", user.getEmail().value());
        assertTrue(user.getRoles().contains(Role.PHOTOGRAPHER));
        assertTrue(user.isActive());
        assertTrue(user.isChangePasswordOnNextLogin());
    }

    @Test
    void shouldRegisterUserCreatedEvent() {
        // When
        User user = User.create("Jan", new Email("jan@photodrive.pl"), HASHED_PASSWORD, Role.PHOTOGRAPHER);

        // Then
        assertFalse(user.pullDomainEvents().isEmpty());
    }

    @Test
    void shouldThrowWhenInactiveUserTriesToLogin() {
        // Given — tworzymy nieaktywnego użytkownika przez deaktywację
        photographer.deactivateUser(false, admin);

        // When & Then
        assertThrows(UserException.class, () -> photographer.login());
    }

    @Test
    void shouldThrowWhenUserMustChangePasswordOnLogin() {
        // Given
        // When & Then
        assertThrows(UserException.class, () -> photographer.login());
    }

    @Test
    void shouldLoginSuccessfullyWhenFlagIsDisabled() {
        // Given
        photographer.setChangePasswordOnNextLogin(false);

        // When & Then
        assertDoesNotThrow(() -> photographer.login());
    }

    @Test
    void shouldAddRoleSuccessfully() {
        // When
        photographer.addRole(Role.ADMIN);

        // Then
        assertTrue(photographer.getRoles().contains(Role.ADMIN));
    }

    @Test
    void shouldThrowWhenAddingDuplicateRole() {
        // When & Then
        assertThrows(UserException.class, () -> photographer.addRole(Role.PHOTOGRAPHER));
    }

    @Test
    void shouldThrowWhenAddingClientRoleToAdmin() {
        // When & Then
        assertThrows(UserException.class, () -> admin.addRole(Role.CLIENT));
    }

    @Test
    void shouldThrowWhenAddingClientRoleToPhotographer() {
        // When & Then
        assertThrows(UserException.class, () -> photographer.addRole(Role.CLIENT));
    }

    @Test
    void shouldRemoveRoleSuccessfully() {
        // Given
        photographer.addRole(Role.ADMIN);

        // When
        photographer.removeRole(Role.PHOTOGRAPHER);

        // Then
        assertFalse(photographer.getRoles().contains(Role.PHOTOGRAPHER));
    }

    @Test
    void shouldThrowWhenRemovingAdminRole() {
        // When & Then
        assertThrows(RuntimeException.class, () -> admin.removeRole(Role.ADMIN));
    }

    @Test
    void shouldThrowWhenRemovingRoleNotAssigned() {
        // When & Then
        assertThrows(UserException.class, () -> photographer.removeRole(Role.CLIENT));
    }

    @Test
    void shouldThrowWhenRemovingLastRole() {
        // Given
        // When & Then
        assertThrows(UserException.class, () -> photographer.removeRole(Role.PHOTOGRAPHER));
    }

    @Test
    void shouldChangePasswordSuccessfully() {
        // Given
        String currentRaw = RAW_PASSWORD;
        String newRaw = "NewPass9!";
        HashedPassword hashedCurrent = new HashedPassword(passwordHasher.encode(currentRaw));
        User user = User.create("X", new Email("x@photodrive.pl"), hashedCurrent, Role.PHOTOGRAPHER);

        // When
        user.changePassword(currentRaw, newRaw, passwordHasher);

        // Then
        assertTrue(passwordHasher.matches(newRaw, user.getPassword().value()));
    }

    @Test
    void shouldThrowWhenCurrentPasswordIsIncorrect() {
        // When & Then
        assertThrows(UserException.class, () ->
                photographer.changePassword("WrongPass1!", "NewPass9!", passwordHasher)
        );
    }

    @Test
    void shouldThrowWhenNewPasswordSameAsCurrent() {
        // Given
        String current = RAW_PASSWORD;
        HashedPassword hashed = new HashedPassword(passwordHasher.encode(current));
        User user = User.create("X", new Email("x2@photodrive.pl"), hashed, Role.PHOTOGRAPHER);

        // When & Then
        assertThrows(UserException.class, () ->
                user.changePassword(current, current, passwordHasher)
        );
    }

    // -------------------------------------------------------------------------
    // changePasswordWithToken
    // -------------------------------------------------------------------------

    @Test
    void shouldChangePasswordWithTokenSuccessfully() {
        // Given
        String newRaw = "NewPass9!";
        UUID token = UUID.randomUUID();

        // When
        photographer.changePasswordWithToken(token, newRaw, passwordHasher);

        // Then
        assertTrue(passwordHasher.matches(newRaw, photographer.getPassword().value()));
    }

    @Test
    void shouldThrowWhenTokenIsNull() {
        // When & Then
        assertThrows(UserException.class, () ->
                photographer.changePasswordWithToken(null, "NewPass9!", passwordHasher)
        );
    }

    @Test
    void shouldThrowWhenNewPasswordSameAsCurrentWithToken() {
        // Given
        String currentRaw = RAW_PASSWORD;
        HashedPassword hashed = new HashedPassword(passwordHasher.encode(currentRaw));
        User user = User.create("X", new Email("x3@photodrive.pl"), hashed, Role.PHOTOGRAPHER);

        // When & Then
        assertThrows(UserException.class, () ->
                user.changePasswordWithToken(UUID.randomUUID(), currentRaw, passwordHasher)
        );
    }

    @Test
    void shouldRegisterUserRemindedPasswordEvent() {
        // Given
        photographer.pullDomainEvents();

        // When
        photographer.changePasswordWithToken(UUID.randomUUID(), "NewPass9!", passwordHasher);

        // Then
        assertFalse(photographer.pullDomainEvents().isEmpty());
    }

    @Test
    void shouldActivateUserSuccessfully() {
        // Given — deaktywujemy najpierw
        photographer.deactivateUser(false, admin);

        // When
        photographer.activeUser(true, admin);

        // Then
        assertTrue(photographer.isActive());
    }

    @Test
    void shouldThrowWhenActivatingAlreadyActiveUser() {
        // When & Then
        assertThrows(UserException.class, () -> photographer.activeUser(true, admin));
    }

    @Test
    void shouldDeactivateUserSuccessfully() {
        // When
        photographer.deactivateUser(false, admin);

        // Then
        assertFalse(photographer.isActive());
    }

    @Test
    void shouldThrowWhenDeactivatingAlreadyInactiveUser() {
        // Given
        photographer.deactivateUser(false, admin);

        // When & Then
        assertThrows(UserException.class, () -> photographer.deactivateUser(false, admin));
    }

    @Test
    void shouldThrowWhenClientTriesToActivateUser() {
        // When & Then
        assertThrows(UserException.class, () -> photographer.activeUser(true, client));
    }

    @Test
    void shouldAssignUsersToPhotographerSuccessfully() {
        // Given
        List<UserId> users = List.of(client.getId());

        // When
        photographer.assignUsers(users, admin);

        // Then
        assertTrue(photographer.getAssignedUsers().contains(client.getId()));
    }

    @Test
    void shouldThrowWhenAssigningUsersToNonPhotographer() {
        // When & Then
        assertThrows(UserException.class, () ->
                admin.assignUsers(List.of(client.getId()), admin)
        );
    }

    @Test
    void shouldThrowWhenNonAdminTriesToAssignUsers() {
        // When & Then
        assertThrows(UserException.class, () ->
                photographer.assignUsers(List.of(client.getId()), photographer)
        );
    }

    @Test
    void shouldDisconnectUsersSuccessfully() {
        // Given
        photographer.assignUsers(new ArrayList<>(List.of(client.getId())), admin);

        // When
        photographer.disconnectUsers(List.of(client.getId()));

        // Then
        assertFalse(photographer.getAssignedUsers().contains(client.getId()));
    }

    @Test
    void shouldThrowWhenDisconnectingUserNotAssigned() {
        // Given
        // When & Then
        assertThrows(UserException.class, () ->
                photographer.disconnectUsers(List.of(client.getId()))
        );
    }

    @Test
    void shouldDoNothingWhenDisconnectingEmptyList() {
        // When & Then
        assertDoesNotThrow(() -> photographer.disconnectUsers(List.of()));
    }

    // -------------------------------------------------------------------------
    // changeEmail
    // -------------------------------------------------------------------------

    @Test
    void shouldChangeEmailSuccessfully() {
        // When
        photographer.changeEmail("new@photodrive.pl");

        // Then
        assertEquals("new@photodrive.pl", photographer.getEmail().value());
    }

    @Test
    void shouldThrowWhenNewEmailSameAsCurrent() {
        assertThrows(UserException.class, () ->
                photographer.changeEmail("photographer@photodrive.pl")
        );
    }

    // -------------------------------------------------------------------------
    // verifyPassword
    // -------------------------------------------------------------------------

    @Test
    void shouldPassVerifyPasswordWhenCorrect() {
        // Given
        String currentRaw = RAW_PASSWORD;
        HashedPassword hashed = new HashedPassword(passwordHasher.encode(currentRaw));
        User user = User.create("V", new Email("v@photodrive.pl"), hashed, Role.PHOTOGRAPHER);

        // When/Then
        assertDoesNotThrow(() -> user.verifyPassword(currentRaw, passwordHasher));
    }

    @Test
    void shouldThrowWhenVerifyPasswordWithWrongPassword() {
        assertThrows(UserException.class, () ->
                photographer.verifyPassword("WrongPass1!", passwordHasher)
        );
    }

    // -------------------------------------------------------------------------
    // shouldChangePasswordOnNextLogin
    // -------------------------------------------------------------------------

    @Test
    void shouldThrowWhenChangePasswordFlagIsSet() {
        // Given — flag is true by default after create()
        assertThrows(UserException.class, () -> photographer.shouldChangePasswordOnNextLogin());
    }

    @Test
    void shouldPassWhenChangePasswordFlagIsCleared() {
        // Given
        photographer.setChangePasswordOnNextLogin(false);

        // When/Then
        assertDoesNotThrow(() -> photographer.shouldChangePasswordOnNextLogin());
    }

    // -------------------------------------------------------------------------
    // hasAccessToReadAllUsers
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnTrueForAdminHasAccessToReadAllUsers() {
        assertTrue(photographer.hasAccessToReadAllUsers(admin));
    }

    @Test
    void shouldThrowForNonAdminHasAccessToReadAllUsers() {
        assertThrows(UserException.class, () ->
                photographer.hasAccessToReadAllUsers(photographer)
        );
    }

    // -------------------------------------------------------------------------
    // getPhotographUsers
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnAssignedUsersForPhotographer() {
        // Given
        photographer.assignUsers(new ArrayList<>(List.of(client.getId())), admin);

        // When
        List<UserId> users = photographer.getPhotographUsers(photographer);

        // Then
        assertTrue(users.contains(client.getId()));
    }

    @Test
    void shouldThrowWhenNonPhotographerCallsGetPhotographUsers() {
        assertThrows(UserException.class, () ->
                photographer.getPhotographUsers(admin)
        );
    }

    @Test
    void shouldThrowWhenPhotographerAccessesAnotherPhotographersUsers() {
        // Given
        User anotherPhotographer = User.create("Other", new Email("other@photodrive.pl"), HASHED_PASSWORD, Role.PHOTOGRAPHER);

        // When/Then — anotherPhotographer tries to read photographer's list
        assertThrows(UserException.class, () ->
                photographer.getPhotographUsers(anotherPhotographer)
        );
    }
}