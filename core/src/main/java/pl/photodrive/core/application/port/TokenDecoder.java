package pl.photodrive.core.application.port;

public interface TokenDecoder {
    AuthenticatedUser parse(String rawJwt);
}
