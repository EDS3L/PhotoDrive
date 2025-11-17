package pl.photodrive.core.application.port;

import pl.photodrive.core.domain.vo.Email;

public interface UserUniquenessChecker {
    boolean isEmailTaken(Email email);
}
