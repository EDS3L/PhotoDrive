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
import pl.photodrive.core.application.exception.ApplicationSecurityException;
import pl.photodrive.core.application.exception.SecurityException;
import pl.photodrive.core.application.port.file.FileStoragePort;
import pl.photodrive.core.application.port.file.FileUniquenessChecker;
import pl.photodrive.core.application.port.repository.AlbumRepository;
import pl.photodrive.core.application.port.repository.FileRepository;
import pl.photodrive.core.application.port.repository.UserRepository;
import pl.photodrive.core.application.port.user.CurrentUser;
import pl.photodrive.core.domain.event.album.FileAddedResult;
import pl.photodrive.core.domain.exception.AlbumException;
import pl.photodrive.core.domain.exception.AlbumNotFoundException;
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
import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
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
    private final FileRepository fileRepository;
    private final ServletContext servletContext;

    @Value("${storage.dir}")
    private Path fileStorageLocation;

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
        validateReadAccess(album, user);

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

        cmd.fileIdList().forEach(fileUuid -> {
            FileId fileId = new FileId(fileUuid);
            album.removeFiles(fileId, user);
        });
        albumRepository.save(album);
        publishEvents(album);
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

        if (!album.canRead(user.getId(), user.getRoles())) {
            throw new SecurityException("User has no access to this album");
        }

        User photographer = getUser(new UserId(album.getPhotographId()));
        return getFileResource(cmd.fileName(),
                album.getFilePath(user, photographer.getEmail().value()),
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

        List<File> removedFiles = album.swapFiles(loggedInUser, targetAlbum.getAlbumPath(), fileIdList);

        Map<FileId, File> incomingFiles = new LinkedHashMap<>();
        for (File file : removedFiles) {
            incomingFiles.put(file.getFileId(), file);
        }
        targetAlbum.receiveFiles(incomingFiles);

        albumRepository.save(album);
        albumRepository.save(targetAlbum);

        publishEvents(album);

    }

    private FileResource getFileResource(String fileName, String filePath, Integer width, Integer height) {

        try {
            Path targetPath = fileStorageLocation.resolve(filePath).resolve(fileName).normalize();

            if (!targetPath.startsWith(fileStorageLocation.toAbsolutePath().normalize())) {
                throw new ApplicationSecurityException("Access denied: path traversal detected");
            }

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

                if (width > 0 || height > 0) {
                    Path thumbPath = fileStorageLocation.resolve(filePath).resolve(".thumbnails").resolve(fileName).normalize();
                    if (Files.exists(thumbPath)) {
                        return new FileResource(new UrlResource(thumbPath.toUri()), contentType);
                    }
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
        
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();
        
        int finalWidth = width;
        int finalHeight = height;
        
        if (finalWidth > 0 && finalHeight <= 0) {
            finalHeight = (int) (((double) finalWidth / originalWidth) * originalHeight);
        } else if (finalHeight > 0 && finalWidth <= 0) {
            finalWidth = (int) (((double) finalHeight / originalHeight) * originalWidth);
        }
        
        BufferedImage resizedImage = new BufferedImage(finalWidth, finalHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = resizedImage.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, 0, 0, finalWidth, finalHeight, null);
        g2d.dispose();

        String filename = originalResource.getFilename();
        String format = filename != null && filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase()
                : "jpg";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (format.equals("jpg") || format.equals("jpeg")) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (writers.hasNext()) {
                ImageWriter writer = writers.next();
                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(0.7f); // 70% quality to significantly reduce file size
                }
                try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                    writer.setOutput(ios);
                    writer.write(null, new IIOImage(resizedImage, null, null), param);
                }
                writer.dispose();
            } else {
                ImageIO.write(resizedImage, format, baos);
            }
        } else {
            ImageIO.write(resizedImage, format, baos);
        }

        byte[] imageBytes = baos.toByteArray();

        return new ByteArrayResource(imageBytes);
    }


    @Transactional
    public void removeExpiredAlbum() {
        List<Album> expiredAlbumList = albumRepository.findByTtdBeforeAndTtdIsNotNull(Instant.now());

        expiredAlbumList.stream()
                .filter(album -> album.getAlbumPath() != null)
                .forEach(album -> {
                    log.info("Expired album to remove {}", album.getName());
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

        if (loggedUser.hasAccessToReadUserAlbums(loggedUser)) {
            return albumRepository.findAllByPhotographId(loggedUser.getId().value());
        } else if (loggedUser.hasAccessToReadAssignedAlbums(loggedUser)) {
            return albumRepository.findAllByClientId(loggedUser.getId().value());
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
    public Album getPublicAlbum(AlbumId albumId) {
        return albumRepository.findPublicByAlbumId(albumId)
                .orElseThrow(() -> new AlbumNotFoundException("Public album not found"));
    }

    @Transactional(readOnly = true)
    public List<Album> getAllPublicAlbums() {
        return albumRepository.findAllPublic();
    }

    @Transactional(readOnly = true)
    public Album getPublicAlbumByName(String name) {
        return albumRepository.findPublicByName(name)
                .orElseThrow(() -> new AlbumNotFoundException("Public album not found"));
    }

    @Transactional(readOnly = true)
    public FileResource getPublicPhoto(UUID albumIdValue, String fileName) {
        AlbumId albumId = new AlbumId(albumIdValue);
        Album album = albumRepository.findPublicByAlbumId(albumId)
                .orElseThrow(() -> new AlbumNotFoundException("Public album not found"));

        boolean fileExistsInAlbum = album.getPhotos().values().stream()
                .anyMatch(f -> f.getFileName().value().equals(fileName));
        if (!fileExistsInAlbum) {
            throw new AlbumException("File not found");
        }

        return getFileResource(fileName, resolveAlbumStoragePath(album), null, null);
    }

    @Transactional
    public void setAlbumPublic(SetAlbumVisibilityCommand cmd) {
        AlbumId albumId = new AlbumId(cmd.albumId());
        Album album = getAlbum(albumId);
        User admin = getUser(currentUser.requireAuthenticated().userId());

        if (cmd.isPublic()) {
            album.makePublic(admin);
        } else {
            album.makePrivate(admin);
        }

        albumRepository.save(album);
        publishEvents(album);
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

    private void validateReadAccess(Album album, User user) {
        if (!album.canRead(user.getId(), user.getRoles())) {
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

            String storagePath = resolveAlbumStoragePath(album);
            eventPublisher.publishEvent(new FileStorageRequested(storagePath,
                    result.file().getFileName(),
                    upload.tempId()));
        }
    }

}
