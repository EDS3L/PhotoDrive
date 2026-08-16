package pl.photodrive.core.application.port.repository;

import java.util.UUID;

public record PublicAlbumSummary(UUID albumId,
                                 String name,
                                 String displayName,
                                 int displayOrder,
                                 long visibleCount) {
}
