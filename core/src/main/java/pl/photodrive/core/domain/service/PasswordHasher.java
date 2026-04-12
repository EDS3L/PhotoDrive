package pl.photodrive.core.domain.service;

public interface PasswordHasher {
    String encode(CharSequence raw);

    boolean matches(CharSequence raw, String hashed);
}
