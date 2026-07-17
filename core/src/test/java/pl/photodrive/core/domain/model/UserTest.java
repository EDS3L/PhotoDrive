package pl.photodrive.core.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.photodrive.core.domain.event.user.PhotographerEmailChanged;
import pl.photodrive.core.domain.service.PasswordHasher;
import pl.photodrive.core.domain.exception.DomainSecurityException;
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
    @DisplayName("New user is created active, with the given role and a pending password change")
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
    @DisplayName("Creating a user registers an event so a photographer gets his storage folder")
    void shouldRegisterUserCreatedEvent() {
        // When
        User user = User.create("Jan", new Email("jan@photodrive.pl"), HASHED_PASSWORD, Role.PHOTOGRAPHER);

        // Then
        assertFalse(user.pullDomainEvents().isEmpty());
    }

    @Test
    @DisplayName("Deactivated user cannot log in")
    void shouldThrowWhenInactiveUserTriesToLogin() {
        // Given - an inactive user, produced by deactivating him first
        photographer.deactivateUser(false, admin);

        // When & Then
        assertThrows(UserException.class, () -> photographer.login());
    }

    @Test
    @DisplayName("Login is refused while the password change flag is still set")
    void shouldThrowWhenUserMustChangePasswordOnLogin() {
        // Given
        // When & Then
        assertThrows(UserException.class, () -> photographer.login());
    }

    @Test
    @DisplayName("Login succeeds once the password change flag is cleared")
    void shouldLoginSuccessfullyWhenFlagIsDisabled() {
        // Given
        photographer.setChangePasswordOnNextLogin(false);

        // When & Then
        assertDoesNotThrow(() -> photographer.login());
    }

    @Test
    @DisplayName("Admin can grant an additional role")
    void shouldAddRoleSuccessfully() {
        // When
        photographer.addRole(Role.ADMIN);

        // Then
        assertTrue(photographer.getRoles().contains(Role.ADMIN));
    }

    @Test
    @DisplayName("The same role cannot be granted twice")
    void shouldThrowWhenAddingDuplicateRole() {
        // When & Then
        assertThrows(UserException.class, () -> photographer.addRole(Role.PHOTOGRAPHER));
    }

    @Test
    @DisplayName("Admin cannot also be a client")
    void shouldThrowWhenAddingClientRoleToAdmin() {
        // When & Then
        assertThrows(UserException.class, () -> admin.addRole(Role.CLIENT));
    }

    @Test
    @DisplayName("Photographer cannot also be a client")
    void shouldThrowWhenAddingClientRoleToPhotographer() {
        // When & Then
        assertThrows(UserException.class, () -> photographer.addRole(Role.CLIENT));
    }

    @Test
    @DisplayName("Admin can revoke a role")
    void shouldRemoveRoleSuccessfully() {
        // Given
        photographer.addRole(Role.ADMIN);

        // When
        photographer.removeRole(Role.PHOTOGRAPHER);

        // Then
        assertFalse(photographer.getRoles().contains(Role.PHOTOGRAPHER));
    }

    @Test
    @DisplayName("The admin role cannot be revoked")
    void shouldThrowWhenRemovingAdminRole() {
        // When & Then
        assertThrows(RuntimeException.class, () -> admin.removeRole(Role.ADMIN));
    }

    @Test
    @DisplayName("A role that was never granted cannot be revoked")
    void shouldThrowWhenRemovingRoleNotAssigned() {
        // When & Then
        assertThrows(UserException.class, () -> photographer.removeRole(Role.CLIENT));
    }

    @Test
    @DisplayName("The last remaining role cannot be revoked, so no user is left role-less")
    void shouldThrowWhenRemovingLastRole() {
        // Given
        // When & Then
        assertThrows(UserException.class, () -> photographer.removeRole(Role.PHOTOGRAPHER));
    }

    @Test
    @DisplayName("User can change his password with the current one")
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
    @DisplayName("Password change is refused when the current password is wrong")
    void shouldThrowWhenCurrentPasswordIsIncorrect() {
        // When & Then
        assertThrows(UserException.class, () ->
                photographer.changePassword("WrongPass1!", "NewPass9!", passwordHasher)
        );
    }

    @Test
    @DisplayName("New password must differ from the current one")
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
    @DisplayName("User can reset his password with a valid authorization code")
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
    @DisplayName("Password reset without an authorization code is refused")
    void shouldThrowWhenTokenIsNull() {
        // When & Then
        assertThrows(UserException.class, () ->
                photographer.changePasswordWithToken(null, "NewPass9!", passwordHasher)
        );
    }

    @Test
    @DisplayName("Password reset to the same password is refused")
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
    @DisplayName("Password reset registers an event so the user gets a confirmation mail")
    void shouldRegisterUserRemindedPasswordEvent() {
        // Given
        photographer.pullDomainEvents();

        // When
        photographer.changePasswordWithToken(UUID.randomUUID(), "NewPass9!", passwordHasher);

        // Then
        assertFalse(photographer.pullDomainEvents().isEmpty());
    }

    @Test
    @DisplayName("Admin can activate a user")
    void shouldActivateUserSuccessfully() {
        // Given - deactivate him first
        photographer.deactivateUser(false, admin);

        // When
        photographer.activeUser(true, admin);

        // Then
        assertTrue(photographer.isActive());
    }

    @Test
    @DisplayName("Activating an already active user is refused")
    void shouldThrowWhenActivatingAlreadyActiveUser() {
        // When & Then
        assertThrows(UserException.class, () -> photographer.activeUser(true, admin));
    }

    @Test
    @DisplayName("Admin can deactivate a user")
    void shouldDeactivateUserSuccessfully() {
        // When
        photographer.deactivateUser(false, admin);

        // Then
        assertFalse(photographer.isActive());
    }

    @Test
    @DisplayName("Deactivating an already inactive user is refused")
    void shouldThrowWhenDeactivatingAlreadyInactiveUser() {
        // Given
        photographer.deactivateUser(false, admin);

        // When & Then
        assertThrows(UserException.class, () -> photographer.deactivateUser(false, admin));
    }

    @Test
    @DisplayName("Client cannot activate other users")
    void shouldThrowWhenClientTriesToActivateUser() {
        // When & Then
        assertThrows(DomainSecurityException.class, () -> photographer.activeUser(true, client));
    }

    @Test
    @DisplayName("Photographer cannot activate accounts, so the domain matches the ADMIN-only endpoint")
    void shouldDenyPhotographerActivatingAnotherUser() {
        // Given - the account is inactive, so only the authorization check can stop the call
        User target = User.create("Target", new Email("target@photodrive.pl"), HASHED_PASSWORD, Role.CLIENT);
        target.deactivateUser(false, admin);

        // When / Then
        assertThrows(DomainSecurityException.class, () -> target.activeUser(true, photographer));
        assertFalse(target.isActive());
    }

    @Test
    @DisplayName("Photographer cannot deactivate accounts either — activation is reserved for the admin")
    void shouldDenyPhotographerDeactivatingAnotherUser() {
        // Given
        User target = User.create("Target", new Email("target2@photodrive.pl"), HASHED_PASSWORD, Role.CLIENT);

        // When / Then
        assertThrows(DomainSecurityException.class, () -> target.deactivateUser(false, photographer));
        assertTrue(target.isActive());
    }

    @Test
    @DisplayName("Admin can assign clients to a photographer")
    void shouldAssignUsersToPhotographerSuccessfully() {
        // Given
        List<UserId> users = List.of(client.getId());

        // When
        photographer.assignUsers(users, admin);

        // Then
        assertTrue(photographer.getAssignedUsers().contains(client.getId()));
    }

    @Test
    @DisplayName("Clients can be assigned only to a photographer")
    void shouldThrowWhenAssigningUsersToNonPhotographer() {
        // When & Then
        assertThrows(UserException.class, () ->
                admin.assignUsers(List.of(client.getId()), admin)
        );
    }

    @Test
    @DisplayName("Only an admin can assign clients to a photographer")
    void shouldThrowWhenNonAdminTriesToAssignUsers() {
        // When & Then
        assertThrows(DomainSecurityException.class, () ->
                photographer.assignUsers(List.of(client.getId()), photographer)
        );
    }

    @Test
    @DisplayName("Admin can unassign clients from a photographer")
    void shouldDisconnectUsersSuccessfully() {
        // Given
        photographer.assignUsers(new ArrayList<>(List.of(client.getId())), admin);

        // When
        photographer.disconnectUsers(List.of(client.getId()));

        // Then
        assertFalse(photographer.getAssignedUsers().contains(client.getId()));
    }

    @Test
    @DisplayName("Unassigning a client who was never assigned is refused")
    void shouldThrowWhenDisconnectingUserNotAssigned() {
        // Given
        // When & Then
        assertThrows(UserException.class, () ->
                photographer.disconnectUsers(List.of(client.getId()))
        );
    }

    @Test
    @DisplayName("Unassigning an empty list changes nothing")
    void shouldDoNothingWhenDisconnectingEmptyList() {
        // When & Then
        assertDoesNotThrow(() -> photographer.disconnectUsers(List.of()));
    }

    // -------------------------------------------------------------------------
    // changeEmail
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("User can change his email")
    void shouldChangeEmailSuccessfully() {
        // When
        photographer.changeEmail("new@photodrive.pl");

        // Then
        assertEquals("new@photodrive.pl", photographer.getEmail().value());
    }

    @Test
    @DisplayName("New email must differ from the current one")
    void shouldThrowWhenNewEmailSameAsCurrent() {
        // When / Then
        assertThrows(UserException.class, () ->
                photographer.changeEmail("photographer@photodrive.pl")
        );
    }

    @Test
    @DisplayName("A photographer changing his email raises a move-folder event carrying both the old and new address, so his photos follow him on disk (B.33)")
    void shouldRaiseFolderMoveEventWhenPhotographerChangesEmail() {
        // Given - drop the UserCreated event from construction
        photographer.pullDomainEvents();

        // When
        photographer.changeEmail("moved@photodrive.pl");

        // Then - exactly one event, carrying the old address (folder source) and the new (target)
        List<Object> events = photographer.pullDomainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(PhotographerEmailChanged.class, events.get(0));
        PhotographerEmailChanged event = (PhotographerEmailChanged) events.get(0);
        assertEquals("photographer@photodrive.pl", event.oldEmail());
        assertEquals("moved@photodrive.pl", event.newEmail());
    }

    @Test
    @DisplayName("A client changing his email raises no move-folder event, because only photographers own a per-email folder (B.33)")
    void shouldNotRaiseFolderMoveEventWhenNonPhotographerChangesEmail() {
        // Given - drop the UserCreated event from construction
        client.pullDomainEvents();

        // When
        client.changeEmail("moved-client@photodrive.pl");

        // Then - no folder move: a client has no folder of its own
        assertTrue(client.pullDomainEvents().isEmpty());
    }

    // -------------------------------------------------------------------------
    // verifyPassword
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Password verification passes for the correct password")
    void shouldPassVerifyPasswordWhenCorrect() {
        // Given
        String currentRaw = RAW_PASSWORD;
        HashedPassword hashed = new HashedPassword(passwordHasher.encode(currentRaw));
        User user = User.create("V", new Email("v@photodrive.pl"), hashed, Role.PHOTOGRAPHER);

        // When/Then
        assertDoesNotThrow(() -> user.verifyPassword(currentRaw, passwordHasher));
    }

    @Test
    @DisplayName("Password verification fails for a wrong password")
    void shouldThrowWhenVerifyPasswordWithWrongPassword() {
        // When / Then
        assertThrows(UserException.class, () ->
                photographer.verifyPassword("WrongPass1!", passwordHasher)
        );
    }

    // -------------------------------------------------------------------------
    // shouldChangePasswordOnNextLogin
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Password change flag blocks the login path")
    void shouldThrowWhenChangePasswordFlagIsSet() {
        // Given - flag is true by default after create()
        assertThrows(UserException.class, () -> photographer.shouldChangePasswordOnNextLogin());
    }

    @Test
    @DisplayName("Cleared password change flag unblocks the login path")
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
    @DisplayName("Admin may read the full user list")
    void shouldReturnTrueForAdminHasAccessToReadAllUsers() {
        // When / Then
        assertTrue(photographer.hasAccessToReadAllUsers(admin));
    }

    @Test
    @DisplayName("Non-admin may not read the full user list")
    void shouldThrowForNonAdminHasAccessToReadAllUsers() {
        // When / Then
        assertThrows(DomainSecurityException.class, () ->
                photographer.hasAccessToReadAllUsers(photographer)
        );
    }

    // -------------------------------------------------------------------------
    // getPhotographUsers
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Photographer sees the clients assigned to him")
    void shouldReturnAssignedUsersForPhotographer() {
        // Given
        photographer.assignUsers(new ArrayList<>(List.of(client.getId())), admin);

        // When
        List<UserId> users = photographer.getPhotographUsers(photographer);

        // Then
        assertTrue(users.contains(client.getId()));
    }

    @Test
    @DisplayName("Only a photographer can list his assigned clients")
    void shouldThrowWhenNonPhotographerCallsGetPhotographUsers() {
        // When / Then
        assertThrows(DomainSecurityException.class, () ->
                photographer.getPhotographUsers(admin)
        );
    }

    @Test
    @DisplayName("Photographer cannot list another photographer's clients")
    void shouldThrowWhenPhotographerAccessesAnotherPhotographersUsers() {
        // Given
        User anotherPhotographer = User.create("Other", new Email("other@photodrive.pl"), HASHED_PASSWORD, Role.PHOTOGRAPHER);

        // When/Then - anotherPhotographer tries to read photographer's list
        assertThrows(DomainSecurityException.class, () ->
                photographer.getPhotographUsers(anotherPhotographer)
        );
    }
}