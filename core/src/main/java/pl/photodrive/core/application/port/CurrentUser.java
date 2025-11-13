package pl.photodrive.core.application.port;


import java.util.Optional;

public interface CurrentUser {

    Optional<AuthenticatedUser> get();

    default AuthenticatedUser requireAuthenticated() {
        return get().orElseThrow(() -> new IllegalStateException("No authenticated user in context"));
    }
}
