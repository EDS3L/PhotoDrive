package pl.photodrive.core.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.photodrive.core.application.command.auth.LoginCommand;
import pl.photodrive.core.application.exception.LoginFailedException;
import pl.photodrive.core.application.port.TokenEncoder;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.port.UserRepository;
import pl.photodrive.core.domain.port.security.PasswordHasher;
import pl.photodrive.core.domain.vo.Email;

import java.time.Clock;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthManagerService {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenEncoder tokenEncoder;
    private final Clock clock;

    public String login(LoginCommand cmd) {
        Email email = new Email(cmd.email());
        User user = userRepository.findByEmail(email).orElseThrow(() -> new LoginFailedException("Invalid credentials!"));

        if(!passwordHasher.matches(cmd.rawPassword(), user.getPassword().value())) throw new LoginFailedException("Invalid credentials!");

        return tokenEncoder.createAccessToken(user.getId(),user.getRoles(),clock.instant(), Duration.ofMinutes(15));
    }

}
