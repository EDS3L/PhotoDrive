package pl.photodrive.core.presentation.dto.album;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateClientAlbumRequest(@NotBlank String name) {
}
