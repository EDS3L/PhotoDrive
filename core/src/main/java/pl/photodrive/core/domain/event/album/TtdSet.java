package pl.photodrive.core.domain.event.album;

import java.time.Instant;

public record TtdSet(Instant ttd, String email) {
}
