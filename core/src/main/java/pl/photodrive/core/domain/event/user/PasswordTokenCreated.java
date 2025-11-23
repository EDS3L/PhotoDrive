package pl.photodrive.core.domain.event.user;

import java.util.UUID;

public record PasswordTokenCreated(String email, UUID token) {
}
