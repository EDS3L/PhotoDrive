package pl.photodrive.core.domain.event;

import pl.photodrive.core.domain.model.Role;

import java.util.Set;
import java.util.UUID;

public record UserCreated(UUID userId, String email, Set<Role> roles) {
}
