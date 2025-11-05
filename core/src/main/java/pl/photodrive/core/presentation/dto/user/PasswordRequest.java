package pl.photodrive.core.presentation.dto.user;

import java.util.UUID;

public record PasswordRequest(String currentPassword, String newPassword) {
}
