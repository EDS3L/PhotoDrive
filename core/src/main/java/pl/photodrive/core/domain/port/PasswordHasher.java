package pl.photodrive.core.domain.port;

public interface PasswordHasher {
    String encode(CharSequence raw);
    boolean matches(CharSequence raw, String hashed);
}
