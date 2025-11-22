package pl.photodrive.core.infrastructure.security;

import lombok.AllArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import pl.photodrive.core.application.port.password.PasswordHasher;


@Component
@AllArgsConstructor
public class BCryptPasswordEncoderAdapter implements PasswordHasher {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    public String encode(CharSequence raw) {
        return bCryptPasswordEncoder.encode(raw);
    }

    @Override
    public boolean matches(CharSequence raw, String hashed) {
        return bCryptPasswordEncoder.matches(raw,hashed);
    }
}
