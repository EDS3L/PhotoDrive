package pl.photodrive.core.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.photodrive.core.application.command.auth.LoginCommand;
import pl.photodrive.core.application.dto.AccessToken;
import pl.photodrive.core.application.exception.LoginFailedException;
import pl.photodrive.core.application.port.TokenEncoder;
import pl.photodrive.core.domain.exception.UserException;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.application.port.PasswordHasher;

import java.time.Clock;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthManagerService {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenEncoder tokenEncoder;
    private final Clock clock;

    public AccessToken login(LoginCommand cmd) {
        User user = userRepository.findByEmail(cmd.email()).orElseThrow(() -> new LoginFailedException("Invalid credentials!"));

        try {
            user.verifyPassword(cmd.rawPassword(), passwordHasher);
        } catch (UserException e) {
            throw new LoginFailedException("Invalid credentials!");
        }

        Duration ttl = Duration.ofMinutes(15);
        String jwt = tokenEncoder.createAccessToken(user.getId(), user.getRoles(), clock.instant(), Duration.ofMinutes(15));
        return new AccessToken(jwt, ttl);
    }

}
