package pl.photodrive.core.presentation.dto.user;

import java.util.UUID;

public record RemindPasswordRequest(String email, UUID token, String newPassword) {
}
