package pl.photodrive.core.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import pl.photodrive.core.application.command.user.*;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.application.port.user.AuthenticatedUser;
import pl.photodrive.core.application.port.user.CurrentUser;
import pl.photodrive.core.application.port.user.UserUniquenessChecker;
import pl.photodrive.core.application.exception.ApplicationSecurityException;
import pl.photodrive.core.domain.exception.DomainSecurityException;
import pl.photodrive.core.domain.exception.UserException;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.service.PasswordHasher;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.HashedPassword;
import pl.photodrive.core.domain.vo.UserId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private UserUniquenessChecker userUniquenessChecker;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private UserManagementService service;

    private final PasswordHasher realHasher = new PasswordHasher() {
        @Override
        public String encode(CharSequence raw) {
            return "hashed_" + raw;
        }

        @Override
        public boolean matches(CharSequence raw, String hashed) {
            return hashed.equals("hashed_" + raw);
        }
    };

    private User adminUser;
    private User photographerUser;

    @BeforeEach
    void setUp() {
        adminUser = User.create("Admin", new Email("admin@photodrive.pl"),
                new HashedPassword(realHasher.encode("Pass123!")), Role.ADMIN);
        photographerUser = User.create("Photographer", new Email("photographer@photodrive.pl"),
                new HashedPassword("hashed_pwd"), Role.PHOTOGRAPHER);
    }

    private void stubCurrentUserAs(User user) {
        AuthenticatedUser auth = new AuthenticatedUser(user.getId(), user.getRoles(), Instant.now().plusSeconds(900), false);
        given(currentUser.requireAuthenticated()).willReturn(auth);
        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
    }

    // -----------------------------------------------------------------------
    // addUser
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Admin creates a user and the start password is generated server-side")
    void shouldAddUserWhenCalledByAdmin() {
        // Given
        stubCurrentUserAs(adminUser);
        given(userUniquenessChecker.isEmailTaken(any())).willReturn(false);
        given(passwordHasher.encode(any())).willReturn("hashed_Pass123!");

        User newUser = User.create("New", new Email("new@photodrive.pl"), new HashedPassword("hashed_Pass123!"), Role.PHOTOGRAPHER);
        given(userRepository.save(any())).willReturn(newUser);

        AddUserCommand cmd = new AddUserCommand("New", "new@photodrive.pl", Role.PHOTOGRAPHER);

        // When
        User result = service.addUser(cmd);

        // Then
        assertThat(result).isNotNull();
        then(userRepository).should().save(any());
        then(eventPublisher).should(atLeastOnce()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Disconnecting a client who was never assigned to that photographer names the client, because this message is the whole body the admin sees")
    void shouldNameTheClientWhenDisconnectingUnassignedClient() {
        // Given - an admin detaching a client that this photographer never had
        stubCurrentUserAs(adminUser);
        User strangerClient = User.create("Stranger",
                new Email("stranger@photodrive.pl"),
                new HashedPassword("hashed_pwd"),
                Role.CLIENT);
        given(userRepository.findById(photographerUser.getId())).willReturn(Optional.of(photographerUser));
        given(userRepository.findById(strangerClient.getId())).willReturn(Optional.of(strangerClient));

        AssignUserCommand cmd = new AssignUserCommand(List.of(strangerClient.getId().value()),
                photographerUser.getId().value());

        // When / Then - a placeholder here reaches the admin verbatim as the 400 body,
        // so the message must say what went wrong AND which client it was about
        assertThatThrownBy(() -> service.disconnectUsersFromPhotographer(cmd))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("not assigned")
                .hasMessageContaining("stranger@photodrive.pl");
    }

    // -----------------------------------------------------------------------
    // assignUsersToPhotograph / disconnectUsersFromPhotographer
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Only clients can be assigned to a photographer, so a photographer cannot end up owning another photographer")
    void shouldRejectAssigningNonClientToPhotographer() {
        // Given - the target of the assignment is another PHOTOGRAPHER, not a client
        stubCurrentUserAs(adminUser);
        User otherPhotographer = User.create("Other",
                new Email("other-photographer@photodrive.pl"),
                new HashedPassword("hashed_pwd"),
                Role.PHOTOGRAPHER);
        given(userRepository.findById(photographerUser.getId())).willReturn(Optional.of(photographerUser));
        given(userRepository.findById(otherPhotographer.getId())).willReturn(Optional.of(otherPhotographer));

        AssignUserCommand cmd = new AssignUserCommand(List.of(otherPhotographer.getId().value()),
                photographerUser.getId().value());

        // When / Then
        assertThatThrownBy(() -> service.assignUsersToPhotograph(cmd))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("Only clients");
        then(userRepository).should(never()).save(photographerUser);
    }

    @Test
    @DisplayName("An inactive client cannot be assigned, so a photographer never gets a client who is unable to log in")
    void shouldRejectAssigningInactiveClient() {
        // Given - a client whose account has been deactivated
        stubCurrentUserAs(adminUser);
        User inactiveClient = User.create("Inactive",
                new Email("inactive@photodrive.pl"),
                new HashedPassword("hashed_pwd"),
                Role.CLIENT);
        inactiveClient.deactivateUser(false, adminUser);
        given(userRepository.findById(photographerUser.getId())).willReturn(Optional.of(photographerUser));
        given(userRepository.findById(inactiveClient.getId())).willReturn(Optional.of(inactiveClient));

        AssignUserCommand cmd = new AssignUserCommand(List.of(inactiveClient.getId().value()),
                photographerUser.getId().value());

        // When / Then
        assertThatThrownBy(() -> service.assignUsersToPhotograph(cmd))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    @DisplayName("Assigning a client twice leaves one entry, so a repeated assignment does not duplicate the photographer's client list")
    void shouldNotDuplicateAlreadyAssignedClient() {
        // Given - the client is ALREADY on the photographer's list
        stubCurrentUserAs(adminUser);
        User client = User.create("Client",
                new Email("client@photodrive.pl"),
                new HashedPassword("hashed_pwd"),
                Role.CLIENT);
        photographerUser.assignUsersForSelf(List.of(client.getId()));
        given(userRepository.findById(photographerUser.getId())).willReturn(Optional.of(photographerUser));
        given(userRepository.findById(client.getId())).willReturn(Optional.of(client));

        AssignUserCommand cmd = new AssignUserCommand(List.of(client.getId().value()),
                photographerUser.getId().value());

        // When - the same client is assigned a second time
        service.assignUsersToPhotograph(cmd);

        // Then
        assertThat(photographerUser.getAssignedUsers()).containsExactly(client.getId());
        then(userRepository).should().save(photographerUser);
    }

    @Test
    @DisplayName("Only an admin may detach clients from a photographer, so a photographer cannot rearrange assignments themselves")
    void shouldRejectDisconnectByNonAdmin() {
        // Given - the photographer is the one calling
        stubCurrentUserAs(photographerUser);

        AssignUserCommand cmd = new AssignUserCommand(List.of(UUID.randomUUID()),
                photographerUser.getId().value());

        // When / Then
        assertThatThrownBy(() -> service.disconnectUsersFromPhotographer(cmd))
                .isInstanceOf(ApplicationSecurityException.class);
        then(userRepository).should(never()).save(any());
    }

    // -----------------------------------------------------------------------
    // read path: assigned clients (B.22)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("One orphaned assignment is skipped instead of failing the whole list, so a single missing row cannot hide every client a photographer has")
    void shouldSkipOrphanedAssignmentWhenListingPhotographerClients() {
        // Given - two assigned ids, but only one of them still exists in the database
        User livingClient = User.create("Living",
                new Email("living@photodrive.pl"),
                new HashedPassword("hashed_pwd"),
                Role.CLIENT);
        UserId orphanId = new UserId(UUID.randomUUID());
        photographerUser.assignUsersForSelf(List.of(livingClient.getId(), orphanId));
        stubCurrentUserAs(photographerUser);
        given(userRepository.findById(livingClient.getId())).willReturn(Optional.of(livingClient));
        given(userRepository.findById(orphanId)).willReturn(Optional.empty());

        // When
        List<User> clients = service.getPhotographUsers();

        // Then - resolving through orElseThrow would blow the whole request up and the
        // photographer would see an empty client list instead of the one client he has
        assertThat(clients).extracting(u -> u.getEmail().value())
                .containsExactly("living@photodrive.pl");
    }

    @Test
    @DisplayName("Admin browsing a photographer's clients also survives an orphaned assignment")
    void shouldSkipOrphanedAssignmentWhenAdminListsPhotographerClients() {
        // Given
        stubCurrentUserAs(adminUser);
        User livingClient = User.create("Living",
                new Email("living@photodrive.pl"),
                new HashedPassword("hashed_pwd"),
                Role.CLIENT);
        UserId orphanId = new UserId(UUID.randomUUID());
        photographerUser.assignUsersForSelf(List.of(orphanId, livingClient.getId()));
        given(userRepository.findById(photographerUser.getId())).willReturn(Optional.of(photographerUser));
        given(userRepository.findById(livingClient.getId())).willReturn(Optional.of(livingClient));
        given(userRepository.findById(orphanId)).willReturn(Optional.empty());

        // When
        List<User> clients = service.getPhotographerUsersForAdmin(photographerUser.getId().value());

        // Then
        assertThat(clients).hasSize(1);
    }

    @Test
    @DisplayName("Only an admin may browse a photographer's client list")
    void shouldRejectPhotographerClientListingByNonAdmin() {
        // Given
        stubCurrentUserAs(photographerUser);

        // When / Then
        assertThatThrownBy(() -> service.getPhotographerUsersForAdmin(photographerUser.getId().value()))
                .isInstanceOf(DomainSecurityException.class);
    }

    @Test
    @DisplayName("Asking for the client list of a user who is not a photographer is a rule violation, not an empty result")
    void shouldRejectClientListingForNonPhotographer() {
        // Given - the id points at a CLIENT, so there is no assignment list to speak of
        stubCurrentUserAs(adminUser);
        User client = User.create("Client",
                new Email("client@photodrive.pl"),
                new HashedPassword("hashed_pwd"),
                Role.CLIENT);
        given(userRepository.findById(client.getId())).willReturn(Optional.of(client));

        // When / Then
        assertThatThrownBy(() -> service.getPhotographerUsersForAdmin(client.getId().value()))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("not a photographer");
    }

    // -----------------------------------------------------------------------
    // activation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Activating an already active account is refused, so the admin learns the state instead of silently doing nothing")
    void shouldRejectActivatingAlreadyActiveUser() {
        // Given - freshly created accounts are active
        stubCurrentUserAs(adminUser);
        given(userRepository.findById(photographerUser.getId())).willReturn(Optional.of(photographerUser));

        // When / Then
        assertThatThrownBy(() -> service.activateUser(
                new ActivateUserCommand(photographerUser.getId().value(), true)))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("already active");
    }

    @Test
    @DisplayName("Deactivation flips the account off and is persisted, so the login check has something to reject")
    void shouldDeactivateActiveUser() {
        // Given
        stubCurrentUserAs(adminUser);
        given(userRepository.findById(photographerUser.getId())).willReturn(Optional.of(photographerUser));
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // When
        service.deactivateUser(new ActivateUserCommand(photographerUser.getId().value(), false));

        // Then
        assertThat(photographerUser.isActive()).isFalse();
        then(userRepository).should().save(photographerUser);
    }

    @Test
    @DisplayName("Only an admin may switch accounts on or off, so a photographer cannot deactivate anybody")
    void shouldRejectDeactivationByPhotographer() {
        // Given
        stubCurrentUserAs(photographerUser);
        User client = User.create("Client",
                new Email("client@photodrive.pl"),
                new HashedPassword("hashed_pwd"),
                Role.CLIENT);
        given(userRepository.findById(client.getId())).willReturn(Optional.of(client));

        // When / Then
        assertThatThrownBy(() -> service.deactivateUser(
                new ActivateUserCommand(client.getId().value(), false)))
                .isInstanceOf(DomainSecurityException.class);
    }

    @Test
    @DisplayName("Listing active users hides deactivated accounts, so an admin does not hand albums to somebody who cannot log in")
    void shouldFilterOutInactiveUsersFromActiveListing() {
        // Given
        stubCurrentUserAs(adminUser);
        User inactive = User.create("Inactive",
                new Email("inactive@photodrive.pl"),
                new HashedPassword("hashed_pwd"),
                Role.CLIENT);
        inactive.deactivateUser(false, adminUser);
        given(userRepository.findAll()).willReturn(List.of(adminUser, inactive));

        // When
        List<User> active = service.getAllActiveUsers();

        // Then
        assertThat(active).extracting(u -> u.getEmail().value())
                .containsExactly("admin@photodrive.pl");
    }

    @Test
    @DisplayName("Email must be unique")
    void shouldThrowWhenEmailAlreadyTaken() {
        // Given
        stubCurrentUserAs(adminUser);
        given(userUniquenessChecker.isEmailTaken(any())).willReturn(true);

        AddUserCommand cmd = new AddUserCommand("X", "dup@photodrive.pl", Role.PHOTOGRAPHER);

        // When / Then
        assertThatThrownBy(() -> service.addUser(cmd))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Client cannot create users")
    void shouldThrowWhenClientTriesToAddUser() {
        // Given
        User clientUser = User.create("Client", new Email("client@photodrive.pl"),
                new HashedPassword("h"), Role.CLIENT);
        stubCurrentUserAs(clientUser);
        given(userUniquenessChecker.isEmailTaken(any())).willReturn(false);

        AddUserCommand cmd = new AddUserCommand("X", "x@photodrive.pl", Role.PHOTOGRAPHER);

        // When / Then - a denial, not a broken business rule: it must reach the client as 403
        assertThatThrownBy(() -> service.addUser(cmd))
                .isInstanceOf(ApplicationSecurityException.class)
                .hasMessageContaining("Only admins or photographer");
    }

    @Test
    @DisplayName("Photographer cannot mint an admin account, so account creation never escalates privileges")
    void shouldDenyPhotographerCreatingPrivilegedAccount() {
        // Given
        User photographerUser = User.create("Photographer", new Email("photographer@photodrive.pl"),
                new HashedPassword("h"), Role.PHOTOGRAPHER);
        stubCurrentUserAs(photographerUser);
        given(userUniquenessChecker.isEmailTaken(any())).willReturn(false);

        AddUserCommand cmd = new AddUserCommand("Escalated", "escalated@photodrive.pl", Role.ADMIN);

        // When / Then
        assertThatThrownBy(() -> service.addUser(cmd))
                .isInstanceOf(ApplicationSecurityException.class)
                .hasMessageContaining("Photographers can only create clients");
        then(userRepository).should(never()).save(any());
    }

    // -----------------------------------------------------------------------
    // changePassword
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("User changes his own password")
    void shouldChangePasswordSuccessfully() {
        // Given
        String currentRaw = "OldPass1!";
        HashedPassword hashed = new HashedPassword(realHasher.encode(currentRaw));
        User user = User.create("U", new Email("u@photodrive.pl"), hashed, Role.PHOTOGRAPHER);

        // Stub current user as the owner of the account
        AuthenticatedUser auth = new AuthenticatedUser(user.getId(), user.getRoles(), Instant.now().plusSeconds(900), false);
        given(currentUser.requireAuthenticated()).willReturn(auth);

        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
        given(passwordHasher.matches(eq(currentRaw), anyString())).willReturn(true);
        given(passwordHasher.matches(eq("NewPass9!"), anyString())).willReturn(false);
        given(passwordHasher.encode("NewPass9!")).willReturn(realHasher.encode("NewPass9!"));
        given(userRepository.save(any())).willReturn(user);

        ChangePasswordCommand cmd = new ChangePasswordCommand(user.getId().value(), currentRaw, "NewPass9!");

        // When / Then
        assertThatCode(() -> service.changePassword(cmd)).doesNotThrowAnyException();
        then(userRepository).should().save(user);
    }

    // -----------------------------------------------------------------------
    // changeEmail - IDOR protection
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Admin can change another user's email")
    void shouldAllowAdminToChangeAnotherUsersEmail() {
        // Given
        stubCurrentUserAs(adminUser);
        given(userRepository.findById(photographerUser.getId())).willReturn(Optional.of(photographerUser));
        given(userRepository.save(any())).willReturn(photographerUser);

        ChangeEmailCommand cmd = new ChangeEmailCommand(
                photographerUser.getId().value(), "newemail@photodrive.pl");

        // When / Then
        assertThatCode(() -> service.changeEmail(cmd)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Non-admin cannot change another user's email")
    void shouldThrowWhenNonAdminTriesToChangeAnotherUsersEmail() {
        // Given - photographerUser tries to change adminUser's email
        AuthenticatedUser auth = new AuthenticatedUser(
                photographerUser.getId(), photographerUser.getRoles(), Instant.now().plusSeconds(900), false);
        given(currentUser.requireAuthenticated()).willReturn(auth);

        ChangeEmailCommand cmd = new ChangeEmailCommand(adminUser.getId().value(), "hacker@photodrive.pl");

        // When / Then
        assertThatThrownBy(() -> service.changeEmail(cmd))
                .isInstanceOf(DomainSecurityException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("User can change his own email")
    void shouldAllowUserToChangeOwnEmail() {
        // Given
        stubCurrentUserAs(photographerUser);
        given(userRepository.save(any())).willReturn(photographerUser);

        ChangeEmailCommand cmd = new ChangeEmailCommand(
                photographerUser.getId().value(), "new@photodrive.pl");

        // When / Then
        assertThatCode(() -> service.changeEmail(cmd)).doesNotThrowAnyException();
    }

    // -----------------------------------------------------------------------
    // getAllUsers / getAllActiveUsers
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Admin can read the full user list")
    void shouldReturnAllUsersForAdmin() {
        // Given
        stubCurrentUserAs(adminUser);
        given(userRepository.findAll()).willReturn(List.of(adminUser, photographerUser));

        // When
        List<User> result = service.getAllUsers();

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Non-admin gets no user list")
    void shouldReturnEmptyListWhenNonAdminCallsGetAllUsers() {
        // Given
        stubCurrentUserAs(photographerUser);

        // When / Then - a denial, not a broken business rule: it must reach the client as 403
        assertThatThrownBy(() -> service.getAllUsers())
                .isInstanceOf(DomainSecurityException.class);
    }

    // -----------------------------------------------------------------------
    // getAllActiveUsers
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Admin can list only the active users")
    void shouldReturnOnlyActiveUsersForAdmin() {
        // Given
        stubCurrentUserAs(adminUser);
        User inactive = new User(
                UserId.newId(), "Inactive", new Email("i@photodrive.pl"),
                new HashedPassword("h"), Set.of(Role.CLIENT), false, false, List.of());
        given(userRepository.findAll()).willReturn(List.of(adminUser, inactive));

        // When
        List<User> result = service.getAllActiveUsers();

        // Then: adminUser is active (default), inactive user is not
        assertThat(result).hasSize(1).containsExactly(adminUser);
    }

    // -----------------------------------------------------------------------
    // addRole / removeRole
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Admin grants a role")
    void shouldAddRoleToUser() {
        // Given - Photographer can receive ADMIN role
        given(userRepository.findById(photographerUser.getId())).willReturn(Optional.of(photographerUser));
        given(userRepository.save(any())).willReturn(photographerUser);

        // When
        User result = service.addRole(new RoleCommand(photographerUser.getId().value(), Role.ADMIN));

        // Then
        then(userRepository).should().save(photographerUser);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Admin revokes a role")
    void shouldRemoveRoleFromUser() {
        // Given - give photographer ADMIN role first so it has 2 roles, then remove PHOTOGRAPHER
        photographerUser.addRole(Role.ADMIN);
        given(userRepository.findById(photographerUser.getId())).willReturn(Optional.of(photographerUser));
        given(userRepository.save(any())).willReturn(photographerUser);

        // When
        User result = service.removeRole(new RoleCommand(photographerUser.getId().value(), Role.PHOTOGRAPHER));

        // Then
        then(userRepository).should().save(photographerUser);
        assertThat(result).isNotNull();
    }

    // -----------------------------------------------------------------------
    // getPhotographUsers
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Photographer reads the clients assigned to him")
    void shouldReturnAssignedUsersForPhotographer() {
        // Given
        stubCurrentUserAs(photographerUser);
        // photographerUser has no assigned users by default - result is empty list
        given(userRepository.findAll()).willReturn(List.of(photographerUser));

        // When
        List<User> result = service.getPhotographUsers();

        // Then
        assertThat(result).isEmpty();
    }
}
