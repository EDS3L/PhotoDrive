package pl.photodrive.core.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.photodrive.core.application.command.user.*;
import pl.photodrive.core.application.port.user.CurrentUser;
import pl.photodrive.core.domain.exception.UserException;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.application.port.user.UserUniquenessChecker;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.application.port.password.PasswordHasher;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.Password;
import pl.photodrive.core.domain.vo.UserId;

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
        if(userUniquenessChecker.isEmailTaken(email)) {
            throw new UserException("User already exists with email: " +email);
        }

        var roles = currentUser.requireAuthenticated().roles();
        boolean isAdmin = roles.contains(Role.ADMIN);
        boolean isPhotographer =  roles.contains(Role.PHOTOGRAPHER);

        if(!isAdmin && !isPhotographer) {
            throw new UserException("Only admins or photographer can add user");
        }
        Password hashedPassword = new Password(passwordHasher.encode(cmd.password()));
        User user = User.create(cmd.name(), email, hashedPassword, cmd.role(), cmd.password());

        var savedUser = userRepository.save(user);

        publishEvents(user);

        return savedUser;
    }

    @Transactional
    public void changePassword(ChangePasswordCommand cmd) {
        UserId userId = new UserId(cmd.userId());
        User user = getUserForDB(userId);
        user.changePassword(cmd.currentPassword(),cmd.newPassword(), passwordHasher);
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
        User user = getUserForDB(userId);
        user.changeEmail(cmd.newEmail());
        return userRepository.save(user);
    }

    @Transactional
    public User activateUser(ActivateUserCommand cmd) {
        UserId userId = new UserId(cmd.userId());
        User authorisedUser =  getUserForDB(currentUser.requireAuthenticated().userId());

        User user = getUserForDB(userId);
        user.activeUser(cmd.active(), authorisedUser);

        return userRepository.save(user);
    }

    @Transactional
    public User deactiveUser(ActivateUserCommand cmd) {
        UserId userId = new UserId(cmd.userId());
        User authorisedUser =  getUserForDB(currentUser.requireAuthenticated().userId());

        User user = getUserForDB(userId);
        user.detectiveUser(cmd.active(), authorisedUser);

        return userRepository.save(user);
    }
    @Transactional
    public void assignUsersToPhotograph(AssignUserCommand cmd) {
        User authorisedUser = getAuthorisedUser();
        UserId userId = new UserId(cmd.userId());
        User photographer = getUserForDB(userId);
        List<UserId> userIdList = cmd.assignedUser().stream().map(UserId::new).toList();
        List<Optional<User>> usersToAssign = userIdList.stream().map(userRepository::findById).toList();

        List<UserId> activeUsers = new ArrayList<>();

        if(!authorisedUser.getRoles().contains(Role.ADMIN)) {
            throw new UserException("Only admins can assign users");
        }

        usersToAssign.forEach(user ->  {
            user.ifPresent(value -> activeUsers.add(value.getId()));
        });

        photographer.assignUsers(activeUsers);

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

        if(!authorisedUser.getRoles().contains(Role.ADMIN)) {
            throw new UserException("Only admins can remove users");
        }

        usersToDisconnect.forEach(user -> {
            if(user.isPresent()) {
                if(new HashSet<>(photographer.getAssignedUsers()).contains(user.get().getId())) {
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

        if(authorisedUser.hasAccessToReadAllUsers(authorisedUser)) {
            List<User> users = userRepository.findAll();
            return users.stream().filter(User::isActive).toList();
        } else {
            return Collections.emptyList();
        }

    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        User authorisedUser = getAuthorisedUser();

        if(authorisedUser.hasAccessToReadAllUsers(authorisedUser)) {
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

        userIdList.forEach(userId ->  {
            User user = getUserForDB(userId);
            users.add(user);
        });

        return users;
    }


    private User getUserForDB(UserId userid) {
        return userRepository.findById(userid).orElseThrow(() -> new UserException("User not found!"));
    }


    private User getAuthorisedUser() {
        return getUserForDB(currentUser.requireAuthenticated().userId());
    }



    private void publishEvents(User user) {
        user.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }
}
