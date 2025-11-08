package pl.photodrive.core.domain.vo;

import pl.photodrive.core.domain.exception.UserException;

import java.util.UUID;

public record UserId(UUID userId) {

    public UserId {
        validate(userId);
    }

    private static void validate(UUID userId) {
        if(userId == null) throw new UserException("User id cannot be null!");
    }

}
