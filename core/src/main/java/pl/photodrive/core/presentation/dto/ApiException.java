package pl.photodrive.core.presentation.dto;

import java.time.Instant;

public record ApiException(String errorCode, String message, Instant timestamp, String path) {
}
