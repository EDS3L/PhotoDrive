package pl.photodrive.core.domain.port;

import pl.photodrive.core.domain.vo.Email;

public interface UserUniquenessChecker {
    boolean isEmailTaken(Email email);
}
