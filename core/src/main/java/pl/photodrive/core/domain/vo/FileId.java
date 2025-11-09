package pl.photodrive.core.domain.vo;

import pl.photodrive.core.domain.exception.UserException;

import java.util.UUID;

public record FileId(UUID value) {

    public FileId {
        validate(value);
    }

    public static UUID newId() {
        return UUID.randomUUID();
    }

    private static void validate(UUID value) {
        if(value == null) throw new UserException("User id cannot be null!");
    }
}
