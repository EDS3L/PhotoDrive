package pl.photodrive.core.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.photodrive.core.application.port.repository.PasswordTokenRepository;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.application.port.user.CurrentUser;
import pl.photodrive.core.domain.exception.UserException;
import pl.photodrive.core.domain.model.PasswordToken;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.vo.Email;


import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenManagementService {

    private final PasswordTokenRepository passwordTokenRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;


    @Transactional
    public void createToken(String email) {
        Instant EXPIRATION_TIME = Instant.now().plusSeconds(900);
        User user = userRepository.findByEmail(new Email(email)).orElseThrow(() -> new UserException("User not found"));

        if(passwordTokenRepository.existsByUserId(user.getId())) {
            PasswordToken passwordToken = passwordTokenRepository.findByUserId(user.getId()).orElseThrow(() -> new UserException("User not found"));
            passwordToken.updateToken(UUID.randomUUID(), email);
            passwordTokenRepository.save(passwordToken);
            publishEvents(passwordToken);
        } else {
            PasswordToken passwordToken = PasswordToken.create(UUID.randomUUID(), EXPIRATION_TIME, Instant.now(), user);
            passwordTokenRepository.save(passwordToken);
            publishEvents(passwordToken);
        }

    }

    private void publishEvents(PasswordToken passwordToken) {
        passwordToken.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }
}
