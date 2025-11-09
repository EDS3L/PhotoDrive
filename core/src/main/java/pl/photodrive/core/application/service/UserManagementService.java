package pl.photodrive.core.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.photodrive.core.application.command.album.CreateAlbumForPhotographer;
import pl.photodrive.core.application.command.user.ChangeEmailCommand;
import pl.photodrive.core.application.command.user.RoleCommand;
import pl.photodrive.core.application.command.user.AddUserCommand;
import pl.photodrive.core.application.command.user.ChangePasswordCommand;
import pl.photodrive.core.domain.exception.UserException;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.port.StoragePort;
import pl.photodrive.core.domain.port.UserUniquenessChecker;
import pl.photodrive.core.domain.port.repository.UserRepository;
import pl.photodrive.core.domain.port.security.PasswordHasher;
import pl.photodrive.core.domain.vo.Password;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AlbumManagementService albumManagementService;
    private final UserUniquenessChecker userUniquenessChecker;

    public User addUser(AddUserCommand cmd) {
        Password hashedPassword = new Password(passwordHasher.encode(cmd.password()));

        User user = User.create(cmd.name(), cmd.email(), hashedPassword, cmd.role(), userUniquenessChecker);

        return userRepository.save(user);
    }

    public void changePassword(ChangePasswordCommand cmd) {
        User user = userRepository.findById(cmd.userId()).orElseThrow(() -> new UserException("User not found!"));
        user.changePassword(cmd.currentPassword(),cmd.newPassword(), passwordHasher);
        userRepository.save(user);
    }

    public User addRole(RoleCommand cmd) {
        User user = userRepository.findById(cmd.id()).orElseThrow(() -> new UserException("User not found!"));
        user.addRole(cmd.role());
        return userRepository.save(user);
    }

    public User removeRole(RoleCommand cmd) {
        User user = userRepository.findById(cmd.id()).orElseThrow(() -> new UserException("User not found!"));
        user.removeRole(cmd.role());
        return userRepository.save(user);
    }

    public User changeEmail(ChangeEmailCommand cmd) {
        User user = userRepository.findById(cmd.id()).orElseThrow(() -> new UserException("User not found!"));
        user.changeEmail(cmd.newEmail());
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
