package pl.photodrive.core.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.photodrive.core.application.command.auth.LoginCommand;
import pl.photodrive.core.application.command.auth.RemindPasswordCommand;
import pl.photodrive.core.application.dto.AccessToken;
import pl.photodrive.core.application.exception.LoginFailedException;
import pl.photodrive.core.domain.service.PasswordHasher;
import pl.photodrive.core.application.port.repository.PasswordTokenRepository;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.application.port.token.TokenEncoder;
import pl.photodrive.core.domain.exception.PasswordTokenException;
import pl.photodrive.core.domain.exception.UserException;
import pl.photodrive.core.domain.model.PasswordToken;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.vo.Email;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

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

    @Value("${app.jwt.access-ttl-minutes:60}")
    private long accessTtlMinutes;

    private static final String INVALID_RESET_TOKEN = "Nieprawidłowy lub wygasły kod autoryzacji.";

    @Transactional
    public AccessToken login(LoginCommand cmd) {
        Email email = new Email(cmd.email());
        User user = getUserByEmail(email);

        if (!user.isActive()) {
            throw new LoginFailedException("Invalid credentials!");
        }

        try {
            user.verifyPassword(cmd.rawPassword(), passwordHasher);
        } catch (UserException e) {
            throw new LoginFailedException("Invalid credentials!");
        }

        Duration ttl = Duration.ofMinutes(accessTtlMinutes);
        String jwt = tokenEncoder.createAccessToken(user.getId(),
                user.getRoles(),
                clock.instant(),
                ttl,
                user.isChangePasswordOnNextLogin());
        return new AccessToken(jwt, ttl);
    }

    @Transactional
    public void remindPassword(RemindPasswordCommand cmd) {
        UUID resetCode = parseResetCode(cmd.token());
        Email email = new Email(cmd.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new PasswordTokenException(INVALID_RESET_TOKEN));

        PasswordToken token = passwordTokenRepository.findByUserId(user.getId())
                .orElseThrow(() -> new PasswordTokenException(INVALID_RESET_TOKEN));

        if (token.getExpiration().isBefore(Instant.now())) throw new PasswordTokenException(INVALID_RESET_TOKEN);
        if (!token.matches(resetCode)) throw new PasswordTokenException(INVALID_RESET_TOKEN);

        user.changePasswordWithToken(resetCode, cmd.newPassword(), passwordHasher);
        user.setChangePasswordOnNextLogin(false);

        passwordTokenRepository.delete(token);

        publishEvents(user);

        userRepository.save(user);

    }

    private UUID parseResetCode(String rawCode) {
        try {
            return UUID.fromString(rawCode.trim());
        } catch (IllegalArgumentException e) {
            throw new PasswordTokenException(INVALID_RESET_TOKEN);
        }
    }

    private User getUserByEmail(Email email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new LoginFailedException("Invalid credentials!"));
    }

    private void publishEvents(User user) {
        user.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }

}
