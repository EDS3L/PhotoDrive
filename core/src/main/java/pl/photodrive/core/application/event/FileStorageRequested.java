package pl.photodrive.core.application.event;

import pl.photodrive.core.domain.vo.FileName;

public record FileStorageRequested(String albumName, FileName fileName, String tempId) {
}