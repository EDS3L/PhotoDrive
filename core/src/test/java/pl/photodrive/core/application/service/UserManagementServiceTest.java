package pl.photodrive.core.application.service;

import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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
        AuthenticatedUser auth = new AuthenticatedUser(user.getId(), user.getRoles(), Instant.now().plusSeconds(900));
        given(currentUser.requireAuthenticated()).willReturn(auth);
        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
    }

    // -----------------------------------------------------------------------
    // addUser
    // -----------------------------------------------------------------------

    @Test
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
        verify(userRepository).save(any());
        verify(eventPublisher, atLeastOnce()).publishEvent(any(Object.class));
    }

    @Test
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
    void shouldThrowWhenClientTriesToAddUser() {
        // Given
        User clientUser = User.create("Client", new Email("client@photodrive.pl"),
                new HashedPassword("h"), Role.CLIENT);
        stubCurrentUserAs(clientUser);
        given(userUniquenessChecker.isEmailTaken(any())).willReturn(false);

        AddUserCommand cmd = new AddUserCommand("X", "x@photodrive.pl", Role.PHOTOGRAPHER);

        // When / Then
        assertThatThrownBy(() -> service.addUser(cmd))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("Only admins or photographer");
    }

    // -----------------------------------------------------------------------
    // changePassword
    // -----------------------------------------------------------------------

    @Test
    void shouldChangePasswordSuccessfully() {
        // Given
        String currentRaw = "OldPass1!";
        HashedPassword hashed = new HashedPassword(realHasher.encode(currentRaw));
        User user = User.create("U", new Email("u@photodrive.pl"), hashed, Role.PHOTOGRAPHER);

        // Stub current user as the owner of the account
        AuthenticatedUser auth = new AuthenticatedUser(user.getId(), user.getRoles(), Instant.now().plusSeconds(900));
        given(currentUser.requireAuthenticated()).willReturn(auth);

        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
        given(passwordHasher.matches(eq(currentRaw), anyString())).willReturn(true);
        given(passwordHasher.matches(eq("NewPass9!"), anyString())).willReturn(false);
        given(passwordHasher.encode("NewPass9!")).willReturn(realHasher.encode("NewPass9!"));
        given(userRepository.save(any())).willReturn(user);

        ChangePasswordCommand cmd = new ChangePasswordCommand(user.getId().value(), currentRaw, "NewPass9!");

        // When / Then
        assertThatCode(() -> service.changePassword(cmd)).doesNotThrowAnyException();
        verify(userRepository).save(user);
    }

    // -----------------------------------------------------------------------
    // changeEmail - IDOR protection
    // -----------------------------------------------------------------------

    @Test
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
    void shouldThrowWhenNonAdminTriesToChangeAnotherUsersEmail() {
        // Given - photographerUser tries to change adminUser's email
        AuthenticatedUser auth = new AuthenticatedUser(
                photographerUser.getId(), photographerUser.getRoles(), Instant.now().plusSeconds(900));
        given(currentUser.requireAuthenticated()).willReturn(auth);

        ChangeEmailCommand cmd = new ChangeEmailCommand(adminUser.getId().value(), "hacker@photodrive.pl");

        // When / Then
        assertThatThrownBy(() -> service.changeEmail(cmd))
                .isInstanceOf(DomainSecurityException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
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
    void shouldReturnEmptyListWhenNonAdminCallsGetAllUsers() {
        // Given
        stubCurrentUserAs(photographerUser);

        // When / Then
        assertThatThrownBy(() -> service.getAllUsers())
                .isInstanceOf(UserException.class);
    }

    // -----------------------------------------------------------------------
    // getAllActiveUsers
    // -----------------------------------------------------------------------

    @Test
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
    void shouldAddRoleToUser() {
        // Given — Photographer can receive ADMIN role
        given(userRepository.findById(photographerUser.getId())).willReturn(Optional.of(photographerUser));
        given(userRepository.save(any())).willReturn(photographerUser);

        // When
        User result = service.addRole(new RoleCommand(photographerUser.getId().value(), Role.ADMIN));

        // Then
        verify(userRepository).save(photographerUser);
        assertThat(result).isNotNull();
    }

    @Test
    void shouldRemoveRoleFromUser() {
        // Given — give photographer ADMIN role first so it has 2 roles, then remove PHOTOGRAPHER
        photographerUser.addRole(Role.ADMIN);
        given(userRepository.findById(photographerUser.getId())).willReturn(Optional.of(photographerUser));
        given(userRepository.save(any())).willReturn(photographerUser);

        // When
        User result = service.removeRole(new RoleCommand(photographerUser.getId().value(), Role.PHOTOGRAPHER));

        // Then
        verify(userRepository).save(photographerUser);
        assertThat(result).isNotNull();
    }

    // -----------------------------------------------------------------------
    // getPhotographUsers
    // -----------------------------------------------------------------------

    @Test
    void shouldReturnAssignedUsersForPhotographer() {
        // Given
        stubCurrentUserAs(photographerUser);
        // photographerUser has no assigned users by default — result is empty list
        given(userRepository.findAll()).willReturn(List.of(photographerUser));

        // When
        List<User> result = service.getPhotographUsers();

        // Then
        assertThat(result).isEmpty();
    }
}
