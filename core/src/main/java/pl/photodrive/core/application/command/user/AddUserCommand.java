package pl.photodrive.core.application.command.user;

import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.vo.Email;
import pl.photodrive.core.domain.vo.Password;

public record AddUserCommand(String name, Email email, Password password, Role role) {
}
