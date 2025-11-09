package pl.photodrive.core.application.command.auth;

import pl.photodrive.core.domain.vo.Email;

public record LoginCommand(Email email, String rawPassword) {
}
