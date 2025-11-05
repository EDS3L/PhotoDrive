package pl.photodrive.core.presentation.dto.user;

import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.vo.Email;

import java.util.Set;
import java.util.UUID;

public record UserDto(UUID id, String name, Email email, Set<Role> roles) {
}
