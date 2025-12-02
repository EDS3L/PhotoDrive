package pl.photodrive.core.presentation.dto.album;

import java.util.List;
import java.util.UUID;

public record ChangeWatermarkRequest(List<UUID> filesUUIDList) {
}
