package pl.photodrive.core.infrastructure.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.UserId;
import pl.photodrive.core.infrastructure.jpa.entity.UserEntity;
import pl.photodrive.core.infrastructure.jpa.mapper.UserEntityMapper;
import pl.photodrive.core.infrastructure.jpa.repository.UserJpaRepository;
import pl.photodrive.core.infrastructure.jpa.vo.user.EmailEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.user.UserIdEmbeddable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {
    private final UserJpaRepository jpa;

    @Override
    public User save(User user) {
        UserEntity savedEntity = jpa.save(UserEntityMapper.toEntity(user));
        return UserEntityMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return jpa.findById(new UserIdEmbeddable(userId.value())).map(UserEntityMapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return jpa.findAll().stream().map(UserEntityMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(new EmailEmbeddable(email.value()));
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpa.findByEmail(new EmailEmbeddable(email.value())).map(UserEntityMapper::toDomain);
    }

}
