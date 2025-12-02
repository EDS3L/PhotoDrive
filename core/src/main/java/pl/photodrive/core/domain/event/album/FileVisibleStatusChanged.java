package pl.photodrive.core.domain.event.album;

import pl.photodrive.core.domain.vo.Email;

public record FileVisibleStatusChanged(Email userEmail, int sizeList) {
}
