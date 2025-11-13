package pl.photodrive.core.domain.event.album;

import pl.photodrive.core.domain.model.File;

public record FileAddedResult(File file, Object event) {
}
