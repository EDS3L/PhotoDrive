package pl.photodrive.core.presentation.dto.user;

public record PasswordRequest(String currentPassword, String newPassword) {
}
