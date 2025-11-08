package pl.photodrive.core.application.port;

import java.util.Optional;

public interface CurrentUser {

    Optional<AuthenticatedUser> get();
}
