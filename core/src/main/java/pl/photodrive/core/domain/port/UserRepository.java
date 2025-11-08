package pl.photodrive.core.domain.port;

import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.UserId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UserId id);
    List<User> findAll();
    boolean existsByEmail(Email email);
}
