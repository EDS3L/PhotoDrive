package pl.photodrive.core.application.port.token;

import pl.photodrive.core.application.port.user.AuthenticatedUser;

public interface TokenDecoder {
    AuthenticatedUser parse(String rawJwt);
}
