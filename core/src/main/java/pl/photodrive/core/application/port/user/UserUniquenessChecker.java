package pl.photodrive.core.application.port.user;

import pl.photodrive.core.domain.vo.Email;

public interface UserUniquenessChecker {
    boolean isEmailTaken(Email email);
}
