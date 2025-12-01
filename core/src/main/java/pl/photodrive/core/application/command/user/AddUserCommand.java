package pl.photodrive.core.application.command.user;

import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.vo.Email;

public record AddUserCommand(String name, String email, String password, Role role) {
}
