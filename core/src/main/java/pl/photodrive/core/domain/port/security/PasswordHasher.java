package pl.photodrive.core.domain.port.security;

public interface PasswordHasher {
    String encode(CharSequence raw);
    boolean matches(CharSequence raw, String hashed);
}
