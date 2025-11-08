package pl.photodrive.core.infrastructure.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.port.UserRepository;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.UserId;
import pl.photodrive.core.infrastructure.jpa.mapper.UserEntityMapper;
import pl.photodrive.core.infrastructure.jpa.repository.UserJpaRepository;
import pl.photodrive.core.infrastructure.jpa.vo.EmailEmbeddable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {
    private final UserJpaRepository jpa;

    @Override
    public User save(User user) {
        return UserEntityMapper.toDomain(jpa.save(UserEntityMapper.toEntity(user)));
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return jpa.findById(userId).map(UserEntityMapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return jpa.findAll().stream().map(UserEntityMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(new EmailEmbeddable(email.value()));
    }

}
