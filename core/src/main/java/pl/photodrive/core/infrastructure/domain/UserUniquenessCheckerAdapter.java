package pl.photodrive.core.infrastructure.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.application.port.user.UserUniquenessChecker;
import pl.photodrive.core.domain.vo.Email;

@Component
@RequiredArgsConstructor
public class UserUniquenessCheckerAdapter implements UserUniquenessChecker {

    private final UserRepository userRepository;

    @Override
    public boolean isEmailTaken(Email email) {
        return userRepository.existsByEmail(email);
    }
}
