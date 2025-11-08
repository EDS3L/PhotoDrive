package pl.photodrive.core.application.port;

import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.vo.UserId;

import java.time.Instant;
import java.util.Set;

public record AuthenticatedUser(UserId userId, Set<Role> roles, Instant expiresAt) {
}
