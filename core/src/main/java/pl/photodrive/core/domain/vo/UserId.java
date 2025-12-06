package pl.photodrive.core.domain.vo;

import pl.photodrive.core.domain.exception.UserException;

import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        validate(value);
    }

    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }

    private static void validate(UUID value) {
        if (value == null) throw new UserException("User id cannot be null!");
    }

}
