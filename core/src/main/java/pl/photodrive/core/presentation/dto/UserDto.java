package pl.photodrive.core.presentation.dto;

import pl.photodrive.core.domain.vo.Email;

import java.util.UUID;

public record UserDto(UUID id, String name, Email email) {
}
