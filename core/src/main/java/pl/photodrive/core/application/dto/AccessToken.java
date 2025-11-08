package pl.photodrive.core.application.dto;

import java.time.Duration;

public record AccessToken(String value, Duration ttl) {
}
