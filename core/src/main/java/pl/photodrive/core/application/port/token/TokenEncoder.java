package pl.photodrive.core.application.port.token;

import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.vo.UserId;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public interface TokenEncoder {
    /**
     * @param mustChangePassword osadza w tokenie flagę wymuszonej zmiany hasła (B.20) — dopóki
     *                           true, filtr wpuszcza tylko odczyt własnego profilu i zmianę hasła.
     */
    String createAccessToken(UserId userId, Set<Role> roles, Instant now, Duration ttl, boolean mustChangePassword);
}
