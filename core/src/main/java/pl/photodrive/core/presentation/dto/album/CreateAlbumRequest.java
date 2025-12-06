package pl.photodrive.core.presentation.dto.album;

import jakarta.validation.constraints.NotBlank;

public record CreateAlbumRequest(@NotBlank String name) {
}
