package pl.photodrive.core.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.photodrive.core.application.command.user.*;
import pl.photodrive.core.domain.service.PasswordHasher;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.application.port.user.CurrentUser;
import pl.photodrive.core.application.port.user.UserUniquenessChecker;
import pl.photodrive.core.application.event.UserCredentialsNotification;
import pl.photodrive.core.domain.exception.DomainSecurityException;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.HashedPassword;
import pl.photodrive.core.domain.vo.Password;
import pl.photodrive.core.domain.vo.UserId;
import pl.photodrive.core.domain.exception.UserException;

import java.util.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final UserUniquenessChecker userUniquenessChecker;
    private final ApplicationEventPublisher eventPublisher;
    private final CurrentUser currentUser;


    @Transactional
    public User addUser(AddUserCommand cmd) {
        Email email = new Email(cmd.email());
        if (userUniquenessChecker.isEmailTaken(email)) {
            throw new UserException("User with this email already exists");
        }

        var authenticated = currentUser.requireAuthenticated();
        var roles = authenticated.roles();
        boolean isAdmin = roles.contains(Role.ADMIN);
        boolean isPhotographer = roles.contains(Role.PHOTOGRAPHER);

        if (!isAdmin && !isPhotographer) {
            throw new UserException("Only admins or photographer can add user");
        }

        if (isPhotographer && !isAdmin && cmd.role() != Role.CLIENT) {
            throw new UserException("Photographers can only create clients");
        }

        new Password(cmd.password()); // walidacja surowego hasła
        HashedPassword hashedPassword = new HashedPassword(passwordHasher.encode(cmd.password()));
        User user = User.create(cmd.name(), email, hashedPassword, cmd.role());

        var savedUser = userRepository.save(user);

        // Auto-assign client to photographer
        if (isPhotographer && cmd.role() == Role.CLIENT) {
            User photographer = getUserForDB(authenticated.userId());
            List<UserId> currentAssigned = new ArrayList<>(photographer.getAssignedUsers());
            currentAssigned.add(savedUser.getId());
            photographer.assignUsersForSelf(currentAssigned);
            userRepository.save(photographer);
        }

        publishEvents(user);

        eventPublisher.publishEvent(new UserCredentialsNotification(cmd.email(), cmd.password()));

        return savedUser;
    }

    @Transactional
    public void changePassword(ChangePasswordCommand cmd) {
        UserId userId = new UserId(cmd.userId());
        var authenticatedUser = currentUser.requireAuthenticated();
        boolean isAdmin = authenticatedUser.roles().contains(Role.ADMIN);
        if (!isAdmin && !authenticatedUser.userId().equals(userId)) {
            throw new DomainSecurityException("Access denied: cannot change another user's password");
        }
        User user = getUserForDB(userId);
        user.changePassword(cmd.currentPassword(), cmd.newPassword(), passwordHasher);
        user.setChangePasswordOnNextLogin(false);
        userRepository.save(user);
    }

    @Transactional
    public User addRole(RoleCommand cmd) {
        UserId userId = new UserId(cmd.userId());
        User user = getUserForDB(userId);
        user.addRole(cmd.role());
        return userRepository.save(user);
    }

    @Transactional
    public User removeRole(RoleCommand cmd) {
        UserId userId = new UserId(cmd.userId());
        User user = getUserForDB(userId);
        user.removeRole(cmd.role());
        return userRepository.save(user);
    }

    @Transactional
    public User changeEmail(ChangeEmailCommand cmd) {
        UserId userId = new UserId(cmd.userId());
        var authenticatedUser = currentUser.requireAuthenticated();
        boolean isAdmin = authenticatedUser.roles().contains(Role.ADMIN);
        if (!isAdmin && !authenticatedUser.userId().equals(userId)) {
            throw new DomainSecurityException("Access denied: cannot change another user's email");
        }
        User user = getUserForDB(userId);
        user.changeEmail(cmd.newEmail());
        return userRepository.save(user);
    }

    @Transactional
    public User activateUser(ActivateUserCommand cmd) {
        UserId userId = new UserId(cmd.userId());
        User authorisedUser = getUserForDB(currentUser.requireAuthenticated().userId());

        User user = getUserForDB(userId);
        user.activeUser(cmd.active(), authorisedUser);

        return userRepository.save(user);
    }

    @Transactional
    public User deactiveUser(ActivateUserCommand cmd) {
        UserId userId = new UserId(cmd.userId());
        User authorisedUser = getUserForDB(currentUser.requireAuthenticated().userId());

        User user = getUserForDB(userId);
        user.deactivateUser(cmd.active(), authorisedUser);

        return userRepository.save(user);
    }

    @Transactional
    public void assignUsersToPhotograph(AssignUserCommand cmd) {
        User authorisedUser = getAuthorisedUser();
        UserId userId = new UserId(cmd.userId());
        User photographer = getUserForDB(userId);
        List<UserId> userIdList = cmd.assignedUser().stream().map(UserId::new).toList();
        List<Optional<User>> usersToAssign = userIdList.stream().map(userRepository::findById).toList();

        List<UserId> activeClients = new ArrayList<>();

        usersToAssign.forEach(user -> {
            user.ifPresent(value -> {
                if (!value.getRoles().contains(Role.CLIENT)) {
                    throw new UserException("Only clients can be assigned to a photographer");
                }
                if (!value.isActive()) {
                    throw new UserException("Cannot assign inactive user: " + value.getName());
                }
                activeClients.add(value.getId());
            });
        });

        photographer.assignUsers(activeClients, authorisedUser);

        userRepository.save(photographer);

    }

    @Transactional
    public void disconnectUsersFromPhotographer(AssignUserCommand cmd) {
        UserId userId = new UserId(cmd.userId());
        User authorisedUser = getAuthorisedUser();
        User photographer = getUserForDB(userId);
        List<UserId> userIdList = cmd.assignedUser().stream().map(UserId::new).toList();
        List<Optional<User>> usersToDisconnect = userIdList.stream().map(userRepository::findById).toList();

        List<UserId> presentUsers = new ArrayList<>();

        if (!authorisedUser.getRoles().contains(Role.ADMIN)) {
            throw new UserException("Only admins can remove users");
        }

        usersToDisconnect.forEach(user -> {
            if (user.isPresent()) {
                if (new HashSet<>(photographer.getAssignedUsers()).contains(user.get().getId())) {
                    presentUsers.add(user.get().getId());
                } else {
                    throw new UserException("User ");
                }
            }
        });

        photographer.disconnectUsers(presentUsers);

        userRepository.save(photographer);
    }

    @Transactional(readOnly = true)
    public List<User> getAllActiveUsers() {
        User authorisedUser = getAuthorisedUser();

        if (authorisedUser.hasAccessToReadAllUsers(authorisedUser)) {
            List<User> users = userRepository.findAll();
            return users.stream().filter(User::isActive).toList();
        } else {
            return Collections.emptyList();
        }

    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        User authorisedUser = getAuthorisedUser();

        if (authorisedUser.hasAccessToReadAllUsers(authorisedUser)) {
            return userRepository.findAll();
        } else {
            return Collections.emptyList();
        }

    }

    @Transactional(readOnly = true)
    public List<User> getPhotographUsers() {
        User authorisedUser = getAuthorisedUser();

        List<UserId> userIdList = authorisedUser.getPhotographUsers(authorisedUser);

        List<User> users = new ArrayList<>();

        userIdList.forEach(userId -> {
            User user = getUserForDB(userId);
            users.add(user);
        });

        return users;
    }

    @Transactional(readOnly = true)
    public List<User> getPhotographerUsersForAdmin(UUID photographerId) {
        User authorisedUser = getAuthorisedUser();
        if (!authorisedUser.getRoles().contains(Role.ADMIN)) {
            throw new DomainSecurityException("Only admins can view photographer assignments");
        }

        User photographer = getUserForDB(new UserId(photographerId));
        if (!photographer.getRoles().contains(Role.PHOTOGRAPHER)) {
            throw new UserException("User is not a photographer");
        }

        List<User> users = new ArrayList<>();
        photographer.getAssignedUsers().forEach(userId -> {
            User user = getUserForDB(userId);
            users.add(user);
        });

        return users;
    }


    private User getUserForDB(UserId userid) {
        return userRepository.findById(userid).orElseThrow(() -> new UserException("User not found!"));
    }


    @Transactional(readOnly = true)
    public User getCurrentUser() {
        return getAuthorisedUser();
    }

    private User getAuthorisedUser() {
        return getUserForDB(currentUser.requireAuthenticated().userId());
    }


    private void publishEvents(User user) {
        user.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }
}
