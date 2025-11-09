package pl.photodrive.core.presentation.dto.album;

import jakarta.validation.constraints.NotBlank;

public record CreateClientAlbumRequest(@NotBlank String name, @NotBlank String clientEmail) {
}
