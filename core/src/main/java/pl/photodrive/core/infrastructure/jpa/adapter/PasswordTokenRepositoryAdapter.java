package pl.photodrive.core.infrastructure.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.photodrive.core.application.port.repository.PasswordTokenRepository;
import pl.photodrive.core.domain.model.PasswordToken;
import pl.photodrive.core.domain.vo.UserId;
import pl.photodrive.core.infrastructure.jpa.mapper.PasswordTokenEntityMapper;
import pl.photodrive.core.infrastructure.jpa.repository.PasswordTokenJpaRepository;
import pl.photodrive.core.infrastructure.jpa.vo.user.UserIdEmbeddable;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PasswordTokenRepositoryAdapter implements PasswordTokenRepository {

    private final PasswordTokenJpaRepository jpa;

    @Override
    public PasswordToken save(PasswordToken passwordToken) {
        return PasswordTokenEntityMapper.toDomain(jpa.save(PasswordTokenEntityMapper.toEntity(passwordToken)));
    }

    @Override
    public boolean existsByUserId(UserId userId) {
        return jpa.existsByUserId(new UserIdEmbeddable(userId.value()));
    }

    @Override
    public Optional<PasswordToken> findByUserId(UserId userId) {
        return Optional.of(PasswordTokenEntityMapper.toDomain(jpa.findByUserId(new UserIdEmbeddable(userId.value()))));
    }
}
