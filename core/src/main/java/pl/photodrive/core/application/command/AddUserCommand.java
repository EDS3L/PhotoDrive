package pl.photodrive.core.application.command;

import pl.photodrive.core.domain.vo.Email;

public record AddUserCommand(String name, Email email, String password) {
}
