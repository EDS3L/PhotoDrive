package pl.photodrive.core.presentation.dto.album;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SetTtdRequest(@NotNull LocalDate ttd) {
}
