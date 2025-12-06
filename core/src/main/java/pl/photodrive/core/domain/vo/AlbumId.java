package pl.photodrive.core.domain.vo;

import pl.photodrive.core.domain.exception.UserException;

import java.util.UUID;

public record AlbumId(UUID value) {
    public AlbumId {
        validate(value);
    }

    public static AlbumId newId() {
        return new AlbumId(UUID.randomUUID());
    }

    private static void validate(UUID value) {
        if (value == null) throw new UserException("User id cannot be null!");
    }

}
