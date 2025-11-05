package pl.photodrive.core.presentation.mapper;

import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.presentation.dto.user.UserDto;

public class ApiMappers {
    public static UserDto toDto(User user) {
        return new UserDto(user.getId(),user.getName(),user.getEmail(), user.getRoles());
    }

}
