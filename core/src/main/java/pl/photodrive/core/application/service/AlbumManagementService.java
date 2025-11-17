package pl.photodrive.core.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.photodrive.core.application.command.album.AddFileToAlbumCommand;
import pl.photodrive.core.application.command.album.CreateAlbumCommand;
import pl.photodrive.core.application.command.album.DownloadFilesCommand;
import pl.photodrive.core.application.command.album.FileUpload;
import pl.photodrive.core.application.event.FileStorageRequested;
import pl.photodrive.core.application.exception.SecurityException;
import pl.photodrive.core.application.port.CurrentUser;
import pl.photodrive.core.application.port.FileStoragePort;
import pl.photodrive.core.domain.event.album.FileAddedResult;
import pl.photodrive.core.domain.exception.AlbumException;
import pl.photodrive.core.domain.exception.UserException;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.model.File;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.application.port.FileUniquenessChecker;
import pl.photodrive.core.application.port.repository.AlbumRepository;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.domain.util.FileNamingPolicy;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.domain.vo.FileName;
import pl.photodrive.core.domain.vo.UserId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumManagementService {

    private final AlbumRepository albumRepository;
    private final UserRepository userRepository;
    private final FileUniquenessChecker fileUniquenessChecker;
    private final ApplicationEventPublisher eventPublisher;
    private final FileStoragePort fileStoragePort;
    private final CurrentUser currentUser;

    @Transactional
    public Album createAdminAlbum(CreateAlbumCommand cmd) {
        checkUserHasRole(currentUser, Role.ADMIN);

        if (albumRepository.existsByName(cmd.albumName())) {
            throw new AlbumException("Album with name '" + cmd.albumName() + "' already exists");
        }

        User admin = getUser(currentUser.requireAuthenticated().userId());

        Album album = Album.createForAdmin(cmd.albumName(), admin);

        Album savedAlbum = albumRepository.save(album);

        publishEvents(album);

        return savedAlbum;

    }

    @Transactional
    public Album createAlbumForClient(CreateAlbumCommand command) {
        checkUserHasRole(currentUser, Role.PHOTOGRAPHER);

        User photographer = getUser(currentUser.requireAuthenticated().userId());
        User client = getUser(new UserId(command.clientId()));

        String fullName = Album.buildClientAlbumName(command.albumName(), client.getEmail());
        if (albumRepository.existsByName(fullName)) {
            throw new AlbumException("Album already exists");
        }

        Album album = Album.createForClient(command.albumName(), photographer, client);

        Album savedAlbum = albumRepository.save(album);
        publishEvents(album);

        return savedAlbum;
    }

    @Transactional
    public List<FileId> addFilesToAlbum(AddFileToAlbumCommand command) {
        Album album = getAlbum(command.albumId());

        User user = getUser(currentUser.requireAuthenticated().userId());

        if (!album.canAccess(user.getId(), user.getRoles())) {
            throw new SecurityException("User has no access to this album");
        }

        List<File> files = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();

        createFilesWithUniqueName(command,album,usedNames,files);

        List<FileAddedResult> results = album.addFiles(files);

        publishUploadedEventsFiles(results,command,user,album);

        albumRepository.save(album);

        publishDomainEvents(results);


        return results.stream().map(r -> r.file().getFileId()).toList();
    }


    @Transactional(readOnly = true)
    public byte[] downloadFilesAsZip(DownloadFilesCommand command) {
        Album album = getAlbum(command.albumId());
        User user = getUser(currentUser.requireAuthenticated().userId());
        validateAccess(album, user);

        List<FileName> fileNames = command.fileNames().stream().map(FileName::new).toList();

        List<File> files = album.getFilesByNames(fileNames);
        if (files.isEmpty()) {
            throw new AlbumException("No files found for download");
        }
        List<String> existingFileNames = files.stream()
                .map(f -> f.getFileName().value())
                .toList();

        String storagePath = resolveAlbumStoragePath(album);
        byte[] zipData = fileStoragePort.createZipArchive(storagePath, existingFileNames);

        log.info("Successfully created ZIP with {} files from album: {}", files.size(), command.albumId().value());

        return zipData;
    }


    private void publishEvents(Album album) {
        album.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }

    private void publishDomainEvents(List<FileAddedResult> results) {
        results.forEach(result -> eventPublisher.publishEvent(result.event()));
    }

    private void checkUserHasRole(CurrentUser currentUser, Role requiredRole) {
        User user = getUser(currentUser.requireAuthenticated().userId());
        if (!user.getRoles().contains(requiredRole)) {
            throw new SecurityException("User does not have required role: " + requiredRole);
        }
    }

    private String resolveAlbumStoragePath(Album album) {
        if (album.getPhotographId().equals(album.getClientId())) {
            return album.getName();
        }

        User photographer = getUser(new UserId(album.getPhotographId()));
        return photographer.getEmail().value() + "/" + album.getName();
    }

    private void validateAccess(Album album, User user) {
        if (!album.canAccess(user.getId(), user.getRoles())) {
            throw new SecurityException("User has no access to this album");
        }
    }

    private User getUser(UserId userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserException("User not found: " + userId.value()));
    }

    private Album getAlbum(AlbumId albumId) {
        return albumRepository.findByAlbumId(albumId).orElseThrow(() -> new AlbumException("Album not found: " + albumId.value()));
    }


    private void createFilesWithUniqueName(AddFileToAlbumCommand command, Album album, Set<String> usedNames, List<File> files ) {
        for (FileUpload upload : command.fileUploads()) {
            FileName requestedName = upload.fileName();

            FileName uniqueName = FileNamingPolicy.makeUnique(requestedName,
                    candidate -> fileUniquenessChecker.isFileNameTaken(album.getAlbumId(),
                            candidate) || usedNames.contains(candidate.value()));

            usedNames.add(uniqueName.value());

            File file = File.create(uniqueName, upload.sizeBytes(), upload.contentType());
            files.add(file);
        }
    }

    private void publishUploadedEventsFiles(List<FileAddedResult> results, AddFileToAlbumCommand command, User user, Album album) {
        for (int i = 0; i < results.size(); i++) {
            FileAddedResult result = results.get(i);
            FileUpload upload = command.fileUploads().get(i);

            if (user.getRoles().contains(Role.ADMIN)) {
                eventPublisher.publishEvent(new FileStorageRequested(album.getName(),
                        result.file().getFileName(),
                        upload.tempId()));
            } else if (user.getRoles().contains(Role.PHOTOGRAPHER)) {
                log.info("[AlbumManagementService] photographer path");
                String path = user.getEmail().value() + "/" + album.getName();
                eventPublisher.publishEvent(new FileStorageRequested(path,
                        result.file().getFileName(),
                        upload.tempId()));
            }
        }
    }

}
