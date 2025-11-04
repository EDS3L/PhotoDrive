package pl.photodrive.core.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.photodrive.core.application.command.AddUserCommand;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.port.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagementService {
    private final UserRepository userRepository;

    public User addUser(AddUserCommand cmd) {
        User user = User.createNew(cmd.name(),cmd.email(),cmd.password());
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
