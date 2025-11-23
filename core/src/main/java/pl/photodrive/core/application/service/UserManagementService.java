package pl.photodrive.core.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.photodrive.core.application.command.user.ChangeEmailCommand;
import pl.photodrive.core.application.command.user.RoleCommand;
import pl.photodrive.core.application.command.user.AddUserCommand;
import pl.photodrive.core.application.command.user.ChangePasswordCommand;
import pl.photodrive.core.application.port.user.CurrentUser;
import pl.photodrive.core.domain.exception.UserException;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.application.port.user.UserUniquenessChecker;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.application.port.password.PasswordHasher;
import pl.photodrive.core.domain.vo.Password;
import pl.photodrive.core.domain.vo.UserId;

import java.util.List;


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
        if(userUniquenessChecker.isEmailTaken(cmd.email())) {
            throw new UserException("User already exists with email: " + cmd.email());
        }

        var roles = currentUser.requireAuthenticated().roles();
        boolean isAdmin = roles.contains(Role.ADMIN);
        boolean isPhotographer =  roles.contains(Role.PHOTOGRAPHER);

        if(!isAdmin && !isPhotographer) {
            throw new UserException("Only admins or photographer can add user");
        }
        Password hashedPassword = new Password(passwordHasher.encode(cmd.password()));
        User user = User.create(cmd.name(), cmd.email(), hashedPassword, cmd.role(), cmd.password());

        var savedUser = userRepository.save(user);

        publishEvents(user);

        return savedUser;
    }

    public void changePassword(ChangePasswordCommand cmd) {
        User user = getUserForDB(cmd.userId());
        user.changePassword(cmd.currentPassword(),cmd.newPassword(), passwordHasher);
        user.setChangePasswordOnNextLogin(false);
        userRepository.save(user);
    }

    public User addRole(RoleCommand cmd) {
        User user = getUserForDB(cmd.userId());
        user.addRole(cmd.role());
        return userRepository.save(user);
    }

    public User removeRole(RoleCommand cmd) {
        User user = getUserForDB(cmd.userId());
        user.removeRole(cmd.role());
        return userRepository.save(user);
    }

    public User changeEmail(ChangeEmailCommand cmd) {
        User user = getUserForDB(cmd.userId());
        user.changeEmail(cmd.newEmail());
        return userRepository.save(user);
    }

    private User getUserForDB(UserId userid) {
        return userRepository.findById(userid).orElseThrow(() -> new UserException("User not found!"));
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    private void publishEvents(User user) {
        user.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }
}
