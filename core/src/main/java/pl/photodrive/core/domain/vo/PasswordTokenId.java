package pl.photodrive.core.domain.vo;

import pl.photodrive.core.domain.exception.UserException;

import java.util.UUID;

public record PasswordTokenId(UUID value) {
    public PasswordTokenId {
        validate(value);
    }

    public static PasswordTokenId newId() {
        return new PasswordTokenId(UUID.randomUUID());
    }

    private static void validate(UUID value) {
        if (value == null) throw new UserException("Password token id cannot be null!");
    }
}
