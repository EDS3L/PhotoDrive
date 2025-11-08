package pl.photodrive.core.application.command.user;

import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.vo.Email;

public record AddUserCommand(String name, Email email, String password, Role role) {
}
