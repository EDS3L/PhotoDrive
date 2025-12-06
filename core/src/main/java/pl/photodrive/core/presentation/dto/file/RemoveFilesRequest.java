package pl.photodrive.core.presentation.dto.file;

import java.util.List;
import java.util.UUID;

public record RemoveFilesRequest(List<UUID> fileIdList) {
}
