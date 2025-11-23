package pl.photodrive.core.application.port.repository;

import pl.photodrive.core.domain.model.PasswordToken;
import pl.photodrive.core.domain.vo.UserId;

import java.util.Optional;

public interface PasswordTokenRepository {
    PasswordToken save(PasswordToken passwordToken);
    boolean existsByUserId(UserId userId);
    Optional<PasswordToken> findByUserId(UserId userId);
}
