package pl.photodrive.core.infrastructure.security;

import lombok.AllArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import pl.photodrive.core.domain.vo.Password;

@Component
@AllArgsConstructor
public class BCryptPasswordEncoderAdapter {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public String encode(Password rawPassword) {
        return bCryptPasswordEncoder.encode(rawPassword.value());
    }

    public boolean matches(String rawPassword, String encodePassword) {
        return bCryptPasswordEncoder.matches(rawPassword,encodePassword);
    }
}
