package pl.photodrive.core.presentation.dto.user;

import java.util.Set;

public record CurrentUserResponse(String id, String name, String email, Set<String> roles,
                                  boolean changePasswordOnNextLogin) {
}
