package pl.photodrive.core.presentation.mapper;

import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.presentation.dto.user.UserDto;

public class ApiMappers {
    public static UserDto toDto(User user) {
        return new UserDto(user.getId().value().toString(),
                user.getName(),
                user.getEmail().value(),
                user.getRoles(),
                user.isActive(),
                user.isChangePasswordOnNextLogin(),
                user.getAssignedUsers().stream()
                        .map(id -> id.value().toString())
                        .toList());
    }

}
