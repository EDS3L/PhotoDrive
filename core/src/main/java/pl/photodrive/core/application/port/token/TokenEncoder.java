package pl.photodrive.core.application.port.token;

import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.vo.UserId;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public interface TokenEncoder {
    String createAccessToken(UserId userId, Set<Role> roles, Instant now, Duration ttl);
}
