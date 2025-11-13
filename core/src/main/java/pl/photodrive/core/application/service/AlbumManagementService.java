package pl.photodrive.core.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.photodrive.core.application.command.album.*;
import pl.photodrive.core.application.exception.SecurityException;
import pl.photodrive.core.application.port.CurrentUser;
import pl.photodrive.core.application.port.FileStoragePort;
import pl.photodrive.core.domain.event.album.FileAddedResult;
import pl.photodrive.core.domain.exception.AlbumException;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.model.File;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.port.FileUniquenessChecker;
import pl.photodrive.core.domain.port.repository.AlbumRepository;
import pl.photodrive.core.domain.port.repository.UserRepository;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.FileName;
import pl.photodrive.core.domain.vo.UserId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


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
    public List<File> addFilesToAlbum(AlbumId albumId, List<MultipartFile> multipartFiles) {
        Album album = getAlbum(albumId);

        User user = getUser(currentUser.requireAuthenticated().userId());

        if (!album.canAccess(user.getId(), user.getRoles())) {
            throw new SecurityException("User has no access to this album");
        }

        List<File> files = convertToFiles(multipartFiles);

        List<FileAddedResult> results = album.addFiles(files);

        albumRepository.save(album);

        publishFileAddedEvents(results, multipartFiles);

        publishEvents(album);

        return results.stream().map(FileAddedResult::file).toList();
    }

    @Transactional(readOnly = true)
    public byte[] downloadFilesAsZip(DownloadFilesCommand command, CurrentUser currentUser) {


        Album album = getAlbum(command.albumId());
        User user = getUser(currentUser.requireAuthenticated().userId());
        validateAccess(album, user);

        List<FileName> fileNames = command.fileNames().stream().map(FileName::new).toList();

        List<File> files = album.getFilesByNames(fileNames);
        if (files.isEmpty()) {
            throw new AlbumException("No files found for download");
        }

        String storagePath = resolveAlbumStoragePath(album);
        byte[] zipData = fileStoragePort.createZipArchive(storagePath, command.fileNames());

        log.info("Successfully created ZIP with {} files from album: {}", files.size(), command.albumId().value());

        return zipData;
    }


    private void publishFileAddedEvents(List<FileAddedResult> results, List<MultipartFile> multipartFiles) {
        for (int i = 0; i < results.size(); i++) {
            FileAddedResult result = results.get(i);
            MultipartFile mpf = multipartFiles.get(i);

            try {
                Object enrichedEvent = enrichEventWithFileData(result.event(), mpf.getInputStream());

                eventPublisher.publishEvent(enrichedEvent);

            } catch (IOException e) {
                log.error("Failed to read file: {}", mpf.getOriginalFilename(), e);
                throw new AlbumException("Failed to process file: " + mpf.getOriginalFilename());
            }
        }
    }

    private List<File> convertToFiles(List<MultipartFile> multipartFiles) {
        List<File> files = new ArrayList<>();

        for (MultipartFile mpf : multipartFiles) {
            if (mpf.isEmpty()) {
                log.warn("Skipping empty file: {}", mpf.getOriginalFilename());
                continue;
            }

            FileName fileName = new FileName(mpf.getOriginalFilename());
            File file = File.create(fileName, mpf.getSize(), mpf.getContentType(), fileUniquenessChecker);

            files.add(file);
        }

        return files;
    }

    private Object enrichEventWithFileData(Object event, java.io.InputStream inputStream) {
        return event;
    }

    private void publishEvents(Album album) {
        album.pullDomainEvents().forEach(eventPublisher::publishEvent);
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
        return userRepository.findById(userId).orElseThrow(() -> new AlbumException("User not found: " + userId.value()));
    }

    private Album getAlbum(AlbumId albumId) {
        return albumRepository.findByAlbumId(albumId).orElseThrow(() -> new AlbumException("Album not found: " + albumId.value()));
    }

}
