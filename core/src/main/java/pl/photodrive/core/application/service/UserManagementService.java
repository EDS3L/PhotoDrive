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

    public User addUser(AddUserCommand cmd) {
        if(userRepository.existsByEmail(cmd.email())) throw new UserException("This email is already taken!");

        Set<Role> roles = new HashSet<>();
        roles.add(cmd.role());
        Password password = new Password(passwordHasher.encode(cmd.password()));
        User user = User.createNew(cmd.name(),cmd.email(),password,roles);

        var newUser = userRepository.save(user);

        createFolderForPhotograph(user);
        return newUser;
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

    private void createFolderForPhotograph(User user) {
        log.info("Tworzymy folder");
        if (user.getRoles().contains(Role.PHOTOGRAPHER)) {
            albumManagementService.createAlbumForPhotographer(new CreateAlbumForPhotographer(user.getEmail().value(),user.getId().value()));
        }
    }


    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
