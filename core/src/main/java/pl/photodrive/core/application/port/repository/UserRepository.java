package pl.photodrive.core.application.port.repository;

import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.UserId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UserId id);
    Optional<User> findByEmail(Email email);
    List<User> findAll();
    boolean existsByEmail(Email email);
}
