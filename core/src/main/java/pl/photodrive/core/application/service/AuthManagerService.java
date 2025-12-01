package pl.photodrive.core.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.photodrive.core.application.command.auth.LoginCommand;
import pl.photodrive.core.application.command.auth.RemindPasswordCommand;
import pl.photodrive.core.application.dto.AccessToken;
import pl.photodrive.core.application.exception.LoginFailedException;
import pl.photodrive.core.application.port.repository.PasswordTokenRepository;
import pl.photodrive.core.application.port.token.TokenEncoder;
import pl.photodrive.core.domain.exception.PasswordTokenException;
import pl.photodrive.core.domain.exception.UserException;
import pl.photodrive.core.domain.model.PasswordToken;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.application.port.password.PasswordHasher;
import pl.photodrive.core.domain.vo.Email;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthManagerService {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenEncoder tokenEncoder;
    private final Clock clock;
    private final PasswordTokenRepository passwordTokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AccessToken login(LoginCommand cmd) {
        Email email = new Email(cmd.email());
        User user = getUserByEmail(email);

        try {
            user.verifyPassword(cmd.rawPassword(), passwordHasher);
        } catch (UserException e) {
            throw new LoginFailedException("Invalid credentials!");
        }

        user.shouldChangePasswordOnNextLogin();
        user.setChangePasswordOnNextLogin(false);
        Duration ttl = Duration.ofMinutes(15);
        String jwt = tokenEncoder.createAccessToken(user.getId(), user.getRoles(), clock.instant(), Duration.ofMinutes(15));
        return new AccessToken(jwt, ttl);
    }

    @Transactional
    public void remindPassword(RemindPasswordCommand cmd) {
        Email email = new Email(cmd.email());
        User user = getUserByEmail(email);

        PasswordToken token = passwordTokenRepository.findByUserId(user.getId()).orElseThrow(() -> new PasswordTokenException("Token not found!"));

        if(token.getExpiration().isBefore(Instant.now())) throw new PasswordTokenException("Token is expired!");
        if(!token.getToken().equals(cmd.token())) throw new PasswordTokenException("Invalid token!");

        user.changePasswordWithToken(cmd.token(), cmd.newPassword(), passwordHasher);

        publishEvents(user);

        userRepository.save(user);

    }

    private User getUserByEmail(Email email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new LoginFailedException("Invalid credentials!"));
    }

    private void publishEvents(User user) {
        user.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }

}
