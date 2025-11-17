package pl.photodrive.core.application.port;

public interface PasswordHasher {
    String encode(CharSequence raw);
    boolean matches(CharSequence raw, String hashed);
}
