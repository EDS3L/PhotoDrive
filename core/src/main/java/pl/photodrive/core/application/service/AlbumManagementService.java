package pl.photodrive.core.application.service;

import jakarta.servlet.ServletContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.photodrive.core.application.command.album.*;
import pl.photodrive.core.application.command.file.ChangeVisibleCommand;
import pl.photodrive.core.application.command.file.FileResource;
import pl.photodrive.core.application.command.file.RemoveFileCommand;
import pl.photodrive.core.application.command.file.RenameFileCommand;
import pl.photodrive.core.application.event.FileStorageRequested;
import pl.photodrive.core.application.exception.SecurityException;
import pl.photodrive.core.application.port.file.FileStoragePort;
import pl.photodrive.core.application.port.file.FileUniquenessChecker;
import pl.photodrive.core.application.port.repository.AlbumRepository;
import pl.photodrive.core.application.port.repository.FileRepository;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.application.port.user.CurrentUser;
import pl.photodrive.core.domain.event.album.FileAddedResult;
import pl.photodrive.core.domain.exception.AlbumException;
import pl.photodrive.core.domain.exception.UserException;
import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.model.File;
import pl.photodrive.core.domain.model.Role;
import pl.photodrive.core.domain.model.User;
import pl.photodrive.core.domain.util.FileNamingPolicy;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.domain.vo.FileName;
import pl.photodrive.core.domain.vo.UserId;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
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
    private final FileRepository fileRepository;

    @Value("${ORG_MAX_SIZE}")
    private long orgMaxSize;

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
    public void deleteAlbum(RemoveAlbumCommand command) {
        AlbumId albumId = new AlbumId(command.albumId());
        Album album = getAlbum(albumId);
        User user = getUser(currentUser.requireAuthenticated().userId());
        User photographer = getUser(new UserId(album.getPhotographId()));

        album.removeFolder(album, user, photographer.getEmail().value());

        albumRepository.delete(album);

        publishEvents(album);
    }

    @Transactional
    public List<FileId> addFilesToAlbum(AddFileToAlbumCommand command) {
        AlbumId albumId = new AlbumId(command.albumId());
        Album album = getAlbum(albumId);

        long orgActualSize = fileRepository.countBySizeBytes();
        log.info("Aktualny rozmiar bayz danych {}", orgActualSize);
        User user = getUser(currentUser.requireAuthenticated().userId());

        if (!album.canAccess(user.getId(), user.getRoles())) {
            throw new SecurityException("User has no access to this album");
        }

        List<File> files = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();

        createFilesWithUniqueName(command, album, usedNames, files);

        List<FileAddedResult> results = album.addFiles(files,orgMaxSize,orgActualSize);

        publishUploadedEventsFiles(results, command, user, album);

        albumRepository.save(album);

        publishDomainEvents(results);


        return results.stream().map(r -> r.file().getFileId()).toList();
    }


    @Transactional(readOnly = true)
    public byte[] downloadFilesAsZip(DownloadFilesCommand command) {
        AlbumId albumId = new AlbumId(command.albumId());
        Album album = getAlbum(albumId);
        User user = getUser(currentUser.requireAuthenticated().userId());
        validateAccess(album, user);

        List<FileName> fileNames = command.fileNames().stream().map(FileName::new).toList();

        album.downloadFiles(fileNames);

        List<File> files = album.getFilesByNames(fileNames);
        if (files.isEmpty()) {
            throw new AlbumException("No files found for download");
        }
        List<String> existingFileNames = files.stream().map(f -> f.getFileName().value()).toList();

        String storagePath = resolveAlbumStoragePath(album);
        byte[] zipData = fileStoragePort.createZipArchive(storagePath, existingFileNames);

        log.info("Successfully created ZIP with {} files from album: {}", files.size(), albumId.value());

        return zipData;
    }

    @Transactional
    public void renameFile(RenameFileCommand cmd) {
        AlbumId albumId = new AlbumId(cmd.albumId());
        FileId fileId = new FileId(cmd.fileId());
        FileName fileName = new FileName(cmd.newFileName());
        User user = getUser(currentUser.requireAuthenticated().userId());
        Album album = albumRepository.findByAlbumId(albumId).orElseThrow(() -> new AlbumException("Album with id '" + cmd.albumId() + "' does not exist"));

        album.renameFile(fileId, fileName, user);

        albumRepository.save(album);

        publishEvents(album);
    }

    @Transactional
    public void removeFiles(RemoveFileCommand cmd) {
        AlbumId albumId = new AlbumId(cmd.albumId());
        User user = getUser(currentUser.requireAuthenticated().userId());
        Album album = albumRepository.findByAlbumId(albumId).orElseThrow(() -> new AlbumException("Album with id '" + cmd.albumId() + "' does not exist"));

        cmd.fileIdList().forEach(FielUuid -> {
            FileId fileId = new FileId(FielUuid);
            album.removeFiles(fileId, user);
            albumRepository.save(album);
            publishEvents(album);
        });
    }

    @Transactional
    public void setTTD(SetTTDCommand cmd) {
        AlbumId albumId = new AlbumId(cmd.albumId());
        Album album = getAlbum(albumId);
        User user = getUser(currentUser.requireAuthenticated().userId());
        UserId clientId = new UserId(album.getClientId());
        User client = getUser(clientId);

        album.setTTD(cmd.ttd(), user, client.getEmail().value());

        albumRepository.save(album);

        publishEvents(album);
    }


    @Transactional(readOnly = true)
    public FileResource getFilePath(GetPhotoPathCommand cmd) {
        AlbumId albumId = new AlbumId(cmd.albumId());
        User user = getUser(currentUser.requireAuthenticated().userId());
        Album album = getAlbum(albumId);

        if (user.getRoles().contains(Role.CLIENT)) {
            User photographer = getUser(new UserId(album.getPhotographId()));
            return getFileResource(cmd.fileName(),
                    album.getFilePath(user, photographer.getEmail().value()),
                    cmd.fileStorageLocation(),
                    cmd.servletContext(),
                    cmd.width(),
                    cmd.height());
        }


        return getFileResource(cmd.fileName(),
                album.getFilePath(user, null),
                cmd.fileStorageLocation(),
                cmd.servletContext(),
                cmd.width(),
                cmd.height());
    }


    @Transactional
    public void swapFile(SwapFileCommand cmd) {

        AlbumId albumId = new AlbumId(cmd.albumId());
        AlbumId targetAlbumId = new AlbumId(cmd.targetAlbumId());
        User loggedInUser = getUser(currentUser.requireAuthenticated().userId());

        Album album = getAlbum(albumId);
        Album targetAlbum = getAlbum(targetAlbumId);

        List<FileId> fileIdList = cmd.fileId().stream().map(FileId::new).toList();

        album.swapFiles(targetAlbum.getPhotos(),loggedInUser,targetAlbum.getAlbumPath(),fileIdList);

        albumRepository.save(album);
        albumRepository.save(targetAlbum);

        publishEvents(album);

    }

    private FileResource getFileResource(String fileName, String filePath, Path fileStorageLocation, ServletContext servletContext, Integer width, Integer height) {

        try {
            Path targetPath = fileStorageLocation.resolve(filePath).resolve(fileName).normalize();

            Resource resource = new UrlResource(targetPath.toUri());


            if (resource.exists() && resource.isReadable()) {

                String contentType = servletContext.getMimeType(resource.getFile().getAbsolutePath());

                if (contentType == null) {
                    if (fileName.toLowerCase().endsWith(".png")) {
                        contentType = "image/png";
                    } else if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
                        contentType = "image/jpeg";
                    } else {
                        contentType = "application/octet-stream";
                    }
                }

                if (width == null) {
                    width = 0;
                }

                if (height == null) {
                    height = 0;
                }

                if (width > 0 && height > 0) {
                    Resource resizedResource = resizeFile(resource, width, height);
                    return new FileResource(resizedResource, contentType);
                }

                return new FileResource(resource, contentType);
            } else {
                throw new AlbumException("File not found");
            }


        } catch (IOException e) {
            throw new AlbumException("Can't read file: " + e.getMessage());
        }
    }

    private Resource resizeFile(Resource originalResource, Integer width, Integer height) throws IOException {
        BufferedImage image = ImageIO.read(originalResource.getInputStream());
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = resizedImage.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, 0, 0, width, height, null);
        g2d.dispose();

        String filename = originalResource.getFilename();
        String format = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, format, baos);
        byte[] imageBytes = baos.toByteArray();

        return new ByteArrayResource(imageBytes);
    }


    @Transactional
    public void removeExpiredAlbum() {
        List<Album> albumList = albumRepository.findAll();
        List<Album> expiredAlbumList = albumList.stream().filter(album -> album.getTtd() != null).filter(album -> album.getAlbumPath() != null).filter(
                album -> album.getTtd().isBefore(Instant.now())).toList();

        expiredAlbumList.forEach(album -> {
            log.info("Expired album to remove {}", album.getName());
        });

        expiredAlbumList.forEach(album -> {
            album.removeExpiredAlbum();
            albumRepository.delete(album);
            publishEvents(album);
        });
    }

    @Transactional
    public void changeVisibleStatus(ChangeVisibleCommand cmd) {
        AlbumId albumId = new AlbumId(cmd.albumId());
        Album album = getAlbum(albumId);
        User user = getUser(currentUser.requireAuthenticated().userId());
        User client = getUser(new UserId(album.getClientId()));

        List<FileId> fileIdList = cmd.fileIds().stream().map(FileId::new).toList();

        album.changeFileVisibleStatus(fileIdList, cmd.isVisible(), user, client.getEmail());

        albumRepository.save(album);

        publishEvents(album);
    }

    @Transactional
    public void changeWatermarkStatus(ChangeWatermarkCommand cmd) {
        User user = getUser(currentUser.requireAuthenticated().userId());
        AlbumId albumId = new AlbumId(cmd.albumId());
        Album album = getAlbum(albumId);

        List<FileId> fileIdList = cmd.filesUUIDList().stream().map(FileId::new).toList();

        album.changeWatermarkStatus(user, cmd.hasWatermark(), fileIdList);

        albumRepository.save(album);

        publishEvents(album);
    }

    @Transactional(readOnly = true)
    public List<Album> getAllAlbums() {
        User loggedUser = getUser(currentUser.requireAuthenticated().userId());

        if (loggedUser.hasAccessToReadAllAlbums(loggedUser)) {
            return albumRepository.findAll();
        }

        throw new AlbumException("Access denied!");
    }

    @Transactional(readOnly = true)
    public List<Album> getAssignedAlbums() {
        User loggedUser = getUser(currentUser.requireAuthenticated().userId());
        List<Album> albumList = albumRepository.findAll();

        if (loggedUser.hasAccessToReadUserAlbums(loggedUser)) {
            return albumList.stream().filter(e -> e.getPhotographId().equals(loggedUser.getId().value())).toList();
        } else if (loggedUser.hasAccessToReadAssignedAlbums(loggedUser)) {
            return albumList.stream().filter(album -> album.getClientId() != null).filter(e -> e.getClientId().equals(
                    loggedUser.getId().value())).toList();
        } else {
            throw new AlbumException("You are not assigned to any album!");
        }

    }

    @Transactional(readOnly = true)
    public List<Album> getAllAlbumsWithoutTTD() {
        return getAllAlbums().stream().filter(album -> album.getTtd() == null).toList();
    }

    @Transactional(readOnly = true)
    public List<Album> getAssignedAlbumsWithoutTTD() {
        return getAssignedAlbums().stream().filter(album -> album.getTtd() == null).toList();
    }

    @Transactional(readOnly = true)
    public List<String> getAllUrlsFromAlbum(GetUrlsCommand cmd) {
        User loggedUser = getUser(currentUser.requireAuthenticated().userId());
        AlbumId albumId = new AlbumId(cmd.albumId());
        Album album = getAlbum(albumId);

        if (!album.hasAccessToGetFilesFromAlbum(loggedUser, cmd.showOnlyVisable()))
            throw new AlbumException("Access denied!");

        List<String> urls = new ArrayList<>();

        album.getPhotos().values().forEach(file -> {
            String fileName = file.getFileName().value();
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            if (cmd.showOnlyVisable()) {
                if (file.isVisible()) {
                    addLinksToList(cmd, album, encodedFileName, urls);
                }
            } else {
                addLinksToList(cmd, album, encodedFileName, urls);
            }
        });

        return urls;
    }


    private void addLinksToList(GetUrlsCommand cmd, Album album, String encodedFileName, List<String> urls) {
        if (cmd.width() == null && cmd.height() == null) {
            urls.add(cmd.domainPath() + "/api/album/" + album.getAlbumId().value() + "/photo/" + encodedFileName);
        } else {
            urls.add(cmd.domainPath() + "/api/album/" + album.getAlbumId().value() + "/photo/" + encodedFileName + "?width=" + cmd.width() + "&height=" + cmd.height());
        }
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


    private void createFilesWithUniqueName(AddFileToAlbumCommand command, Album album, Set<String> usedNames, List<File> files) {
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
                addFileToClientAlbumByAdmin(album, user, result, upload);

                eventPublisher.publishEvent(new FileStorageRequested(album.getName(),
                        result.file().getFileName(),
                        upload.tempId()));
            } else if (user.getRoles().contains(Role.PHOTOGRAPHER)) {
                String path = user.getEmail().value() + "/" + album.getName();
                eventPublisher.publishEvent(new FileStorageRequested(path,
                        result.file().getFileName(),
                        upload.tempId()));
            }
        }
    }

    private void addFileToClientAlbumByAdmin(Album album, User user, FileAddedResult result, FileUpload upload) {
        if (!album.getPhotographId().equals(user.getId().value())) {
            User photographUser = getUser(new UserId(album.getPhotographId()));
            eventPublisher.publishEvent(new FileStorageRequested(photographUser.getEmail().value() + "/" + album.getName(),
                    result.file().getFileName(),
                    upload.tempId()));
        }
    }

}
