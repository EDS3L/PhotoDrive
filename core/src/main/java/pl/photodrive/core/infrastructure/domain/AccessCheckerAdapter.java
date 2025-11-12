package pl.photodrive.core.infrastructure.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import pl.photodrive.core.application.exception.SecurityException;
import pl.photodrive.core.application.port.CurrentUser;
import pl.photodrive.core.domain.exception.AlbumException;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.model.File;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.port.repository.AlbumRepository;
import pl.photodrive.core.domain.port.security.AccessChecker;
import pl.photodrive.core.domain.vo.FileId;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AccessCheckerAdapter implements AccessChecker {

    private final CurrentUser currentUser;
    private final AlbumRepository albumRepository;
    private final ApplicationEventPublisher eventPublisher;


    @Override
    public void iSCurrentUserHasAccess(CurrentUser currentUser, Role role) {
        var userOpt = currentUser.get();

        var user = userOpt.orElseThrow(
                () -> new SecurityException("User not authenticated")
        );

        if (!user.roles().contains(role)) {
            throw new SecurityException("Access denied!");
        }
    }

    @Override
    public void isAlbumExists(String albumName) {
        if (!albumRepository.existsByName(albumName)) throw new AlbumException("Album not found!");
    }

    @Override
    public void isFilesExistsInAlbum(String albumName, CurrentUser currentUser, List<String> fileList) {
        var user = currentUser.get().orElseThrow(
                () -> new SecurityException("User not authenticated")
        );

        Album album = albumRepository.findByName(albumName)
                .orElseThrow(() -> new AlbumException("Album not found"));

        UUID userId = user.userId().value();

        boolean isClient = album.getClientId().equals(userId);
        boolean isPhotograph = album.getPhotographId().equals(userId);

        if (!isClient && !isPhotograph) {
            throw new SecurityException("User has no access to this album");
        }

        Map<FileId, File> fileMap = album.getPhotos();
        if (fileMap == null || fileMap.isEmpty()) {
            throw new AlbumException("Album has no photos");
        }

        Set<String> albumFileNames = fileMap.values().stream()
                .map(f -> f.getFileName().value())
                .collect(Collectors.toSet());

        for (String requestedFileName : fileList) {
            if (!albumFileNames.contains(requestedFileName)) {
                throw new AlbumException(requestedFileName + " not found in " + album.getName());
            }
        }

    }
}
