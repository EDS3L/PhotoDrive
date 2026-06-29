package pl.photodrive.core.domain.model;

import lombok.extern.slf4j.Slf4j;
import pl.photodrive.core.domain.event.album.*;
import pl.photodrive.core.domain.exception.AlbumException;
import pl.photodrive.core.domain.exception.FileException;
import pl.photodrive.core.domain.util.FileNamingPolicy;
import pl.photodrive.core.domain.vo.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class Album {

    private final AlbumId albumId;
    private final String name;
    private final UUID photographId;
    private Instant ttd;
    private boolean isPublic;
    private transient final List<Object> domainEvents = new ArrayList<>();
    private final UUID clientId;
    private Map<FileId, File> photos = new LinkedHashMap<>();
    private final AlbumPath albumPath;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg",
            ".jpeg",
            ".png",
            ".bmp",
            ".webp",
            ".tiff",
            ".heic");


    public Album(AlbumId albumId, String name, UUID photographId, UUID clientId, Instant ttd, AlbumPath albumPath, boolean isPublic) {
        if (name == null) throw new AlbumException("Album name cannot be null!");
        if (photographId == null) throw new AlbumException("Photograph name cannot be null!");
        if (albumPath == null) throw new AlbumException("Album to path cannot be null!");
        this.albumId = albumId;
        this.name = name;
        this.photographId = photographId;
        this.clientId = clientId;
        this.ttd = ttd;
        this.albumPath = albumPath;
        this.isPublic = isPublic;
    }

    public void setTTD(Instant ttd, User user, String email) {
        Instant now = Instant.now();
        boolean isAdmin = user.getRoles().contains(Role.ADMIN);
        boolean isPhotograph = user.getRoles().contains(Role.PHOTOGRAPHER);

        if (isAdmin) {
            if (photographId.equals(user.getId().value()) && clientId.equals(user.getId().value())) {
                throw new AlbumException("Cannot set TTD for admin album");
            }
        }

        if (ttd.isBefore(now)) {
            throw new AlbumException("Cannot set TTD before now!");
        }

        if (isPhotograph && !photographId.equals(user.getId().value())) {
            throw new AlbumException("Photographer can only set TTD on own albums");
        }

        if (isAdmin || isPhotograph) {
            this.ttd = ttd;
            this.registerEvent(new TtdSet(ttd, email));
        } else {
            throw new AlbumException("Access denied!");
        }

    }

    public static Album createForAdmin(String albumName, User admin) {
        if (!admin.getRoles().contains(Role.ADMIN)) {
            throw new AlbumException("Only administrators can create admin albums");
        }

        Album album = new Album(AlbumId.newId(),
                albumName,
                admin.getId().value(),
                admin.getId().value(),
                null,
                new AlbumPath(albumName),
                false);
        album.registerEvent(new AdminAlbumCreated(albumName, admin.getEmail().value()));
        return album;
    }

    public static Album createForClient(String albumName, User photographer, User client) {

        if (!photographer.getRoles().contains(Role.PHOTOGRAPHER)) {
            throw new AlbumException("Only photographers can create client albums");
        }
        if (!client.getRoles().contains(Role.CLIENT)) {
            throw new AlbumException("Album must be assigned to a client");
        }

        String fullAlbumName = buildClientAlbumName(albumName, client.getEmail());
        log.info("Album path: {}", photographer.getEmail().value() + "/" + fullAlbumName);
        Album album = new Album(AlbumId.newId(),
                fullAlbumName,
                photographer.getId().value(),
                client.getId().value(),
                null,
                new AlbumPath(photographer.getEmail().value() + "/" + fullAlbumName),
                false);

        album.registerEvent(new PhotographCreateAlbum(photographer.getEmail().value(), fullAlbumName));
        return album;
    }

    public static String buildClientAlbumName(String baseName, Email clientEmail) {
        return String.format("%s_%s_%s", baseName, clientEmail.value(), LocalDate.now());
    }

    public void removeFolder(Album albumDelete, User currentUser, String photographerEmail) {
        if (albumDelete == null) {
            throw new AlbumException("There's no album to delete");
        }

        if (!(currentUser.getRoles().contains(Role.PHOTOGRAPHER) || currentUser.getRoles().contains(Role.ADMIN))) {
            throw new AlbumException("Only admin or photographer can delete albums");
        }

        boolean isOwner = albumDelete.getPhotographId().equals(currentUser.getId().value());
        boolean isAdmin = currentUser.getRoles().contains(Role.ADMIN);

        if (!(isOwner || isAdmin)) {
            throw new AlbumException("Only admin or album owner can delete the album");
        }

        if (!this.photos.isEmpty()) {
            photos.clear();
        }

        albumDelete.registerEvent(new PhotographRemoveAlbum(albumPath.value()));
    }

    public void removeFiles(FileId fileId, User user) {

        if (!canManageFiles(user)) {
            throw new AlbumException("Only admin or album owner can remove the file");
        }
        boolean isAdmin = user.getRoles().contains(Role.ADMIN);
        boolean isPhotograph = user.getRoles().contains(Role.PHOTOGRAPHER);

        File removedFile = photos.remove(fileId);
        if (removedFile == null) {
            throw new AlbumException("File not found: " + fileId.value());
        }

        if (isAdmin) {
            registerEvent(new FileRemovedFromAlbum(this.name, removedFile.getFileName().value()));
        } else if (isPhotograph) {
            registerEvent(new FileRemovedFromAlbum(user.getEmail().value() + "/" + this.name,
                    removedFile.getFileName().value()));
        }

    }

    public List<FileAddedResult> addFiles(List<File> files, long maxSize, long actualSize) {
        List<FileAddedResult> results = new ArrayList<>();
        long totalSize = files.stream().mapToLong(File::getSizeBytes).sum();

        checkStorage(maxSize, actualSize, totalSize);
        for (File file : files) {
            FileAddedResult result = addFile(file);
            results.add(result);
        }

        return results;
    }

    public FileAddedResult addFile(File file) {
        if (photos.containsKey(file.getFileId())) {
            throw new AlbumException("File already exists in album: " + file.getFileName().value());
        }

        FileName desired = file.getFileName();
        FileName uniqueName = makeUniqueFileName(desired);

        file.rename(uniqueName);
        photos.put(file.getFileId(), file);

        if (isAdminAlbum()) {
            return new FileAddedResult(file, new FileAddedToAlbum(file.getFileId(), file.getFileName(), this.name));
        } else {
            return new FileAddedResult(file,
                    new FileAddedToClientAlbum(file.getFileId(),
                            file.getFileName(),
                            this.name,
                            this.photographId.toString()));
        }
    }

    public void downloadFiles(List<FileName> fileNames) {
        Set<String> availableFileNames = photos.values().stream().map(file -> file.getFileName().value()).collect(
                Collectors.toSet());

        for (FileName requestedFileName : fileNames) {
            if (!availableFileNames.contains(requestedFileName.value())) {
                throw new AlbumException("File" + requestedFileName.value() + " does not exist in this album");
            }
        }
    }

    public void renameFile(FileId fileId, FileName newFileName, User user) {
        File file = photos.get(fileId);
        if (file == null) {
            throw new AlbumException("File not found: " + fileId.value());
        }

        boolean fileExist = photos.values().stream().filter(f -> !f.getFileId().equals(fileId)).anyMatch(f -> f.getFileName().equals(
                newFileName));

        if (fileExist) {
            throw new AlbumException("File with name '" + newFileName.value() + "' already exists in this album.");
        }

        if (!canManageFiles(user)) {
            throw new AlbumException("Only admin or album owner can rename the file");
        }
        boolean isAdmin = user.getRoles().contains(Role.ADMIN);
        boolean isPhotograph = user.getRoles().contains(Role.PHOTOGRAPHER);

        FileName oldFileName = file.getFileName();
        file.rename(newFileName);

        if (isAdmin) {
            registerEvent(new FileRenamedInAlbum(this.name, oldFileName, newFileName));
        } else if (isPhotograph) {
            registerEvent(new FileRenamedInAlbum(user.getEmail().value() + "/" + this.name, oldFileName, newFileName));
        }

    }

    public void removeExpiredAlbum() {
        if (this.ttd.isBefore(Instant.now())) {
            log.info("This album {} is expired", this.name);
            registerEvent(new ExpiredAlbumRemoved(albumPath));
        } else {
            throw new AlbumException("Album is not expired");
        }
    }


    public void changeFileVisibleStatus(List<FileId> fileIdList, boolean isVisible, User user, Email userEmail) {
        if (!canManageFiles(user)) {
            throw new AlbumException("User is not allowed to change file visibility");
        }

        fileIdList.forEach(fileId -> {
            File file = photos.get(fileId);
            if (file == null) {
                throw new AlbumException("File not found: " + fileId.value());
            }

            if (isVisible) {
                file.setViable();
            } else {
                file.setUnviable();
            }

        });

        if (!clientId.equals(photographId)) {
            if (isVisible) {
                this.registerEvent(new FileVisibleStatusChanged(userEmail, fileIdList.size()));
            }
        }

    }

    public void changeWatermarkStatus(User user, boolean hasWatermark, List<FileId> fileIdList) {
        if (!canManageFiles(user)) {
            throw new AlbumException("User is not allowed to change file visibility");
        }

        if (fileIdList.size() > 15) {
            throw new AlbumException("There are more than 15 files in this album");
        }

        fileIdList.forEach(fileId -> {
            File file = photos.get(fileId);

            if (file == null) {
                throw new AlbumException("File not found: " + fileId.value());
            }

            validateExtensions(file);

            if (hasWatermark) {
                file.setWaterMark();
                photos.put(fileId, file);
                this.registerEvent(new WatermarkAddedToPhoto(this.albumPath.value() + "/" + file.getFileName().value()));
            } else {
                file.disableWatermark();
                photos.put(fileId, file);
            }
        });
    }


    private static void validateExtensions(File file) {
        String fileName = file.getFileName().value();
        String lower = fileName.toLowerCase();

        if (ALLOWED_EXTENSIONS.stream().noneMatch(lower::endsWith)) {
            throw new FileException("Invalid or unsupported file format");
        }
    }

    public boolean hasAccessToGetFilesFromAlbum(User currentUser, boolean visible) {
        if (currentUser.getRoles().contains(Role.ADMIN)) {
            return true;
        } else if (currentUser.getRoles().contains(Role.PHOTOGRAPHER)) {
            return this.getPhotographId().equals(currentUser.getId().value());
        } else if (currentUser.getRoles().contains(Role.CLIENT)) {
            if(!visible) throw new AlbumException("Access denied!");
            return this.getClientId().equals(currentUser.getId().value());
        }
        return false;
    }

    public List<File> swapFiles(User currentUser, AlbumPath targetAlbumPath, List<FileId> fileIdList) {
        List<File> removedFiles = new ArrayList<>();
        fileIdList.forEach(fileId -> {
            removedFiles.add(removeFileForSwap(currentUser, targetAlbumPath, fileId));
        });
        return removedFiles;
    }

    private File removeFileForSwap(User currentUser, AlbumPath targetAlbumPath, FileId targetFileId) {
        boolean isAdmin =  currentUser.getRoles().contains(Role.ADMIN);
        boolean isPhotograph =  currentUser.getRoles().contains(Role.PHOTOGRAPHER);

        if(!(isAdmin || isPhotograph)) {
            throw new AlbumException("Only admin or album owner can swap the file");
        }

        if(photos.get(targetFileId) == null) {
            throw new AlbumException("File not found: " + targetFileId.value());
        }

        File file = photos.remove(targetFileId);

        this.registerEvent(new FileSwaped(albumPath, targetAlbumPath, file.getFileName()));

        return file;
    }

    public void receiveFiles(Map<FileId, File> incomingFiles) {
        photos.putAll(incomingFiles);
    }


    public void makePublic(User admin) {
        if (!admin.getRoles().contains(Role.ADMIN)) {
            throw new AlbumException("Only administrators can change album visibility");
        }
        if (!isAdminAlbum()) {
            throw new AlbumException("Only admin albums can be made public");
        }
        if (this.isPublic) return;
        this.isPublic = true;
        registerEvent(new AlbumVisibilityChanged(albumId.value(), true));
    }

    public void makePrivate(User admin) {
        if (!admin.getRoles().contains(Role.ADMIN)) {
            throw new AlbumException("Only administrators can change album visibility");
        }
        if (!isAdminAlbum()) {
            throw new AlbumException("Only admin albums can be made private");
        }
        if (!this.isPublic) return;
        this.isPublic = false;
        registerEvent(new AlbumVisibilityChanged(albumId.value(), false));
    }

    public boolean isPublic() {
        return isPublic;
    }

    public String getFilePath(User user, String photographEmail) {
        boolean isAdmin = user.getRoles().contains(Role.ADMIN);
        boolean isPhotograph = user.getRoles().contains(Role.PHOTOGRAPHER);
        boolean isClient = user.getRoles().contains(Role.CLIENT);

        if (isAdmin && isAdminAlbum() && photographId.equals(user.getId().value())) {
            return this.name;
        } else if (isAdmin && !isAdminAlbum()) {
            return photographEmail + "/" + this.name;
        } else if (isPhotograph && photographId.equals(user.getId().value())) {
            return user.getEmail().value() + "/" + this.name;
        } else if (isClient && clientId.equals(user.getId().value())) {
            return photographEmail + "/" + this.name;
        } else {
            throw new AlbumException("Access denied!");
        }
    }

    public boolean canAccess(UserId userId, Set<Role> userRoles) {
        if (userRoles.contains(Role.ADMIN)) {
            return true;
        }

        return userRoles.contains(Role.PHOTOGRAPHER) && photographId.equals(userId.value());
    }

    public boolean canRead(UserId userId, Set<Role> userRoles) {
        if (canAccess(userId, userRoles)) {
            return true;
        }

        return userRoles.contains(Role.CLIENT) && clientId.equals(userId.value());
    }

    public boolean canReadFile(User user, String fileName) {
        if (canAccess(user.getId(), user.getRoles())) {
            return true;
        }

        if (!user.getRoles().contains(Role.CLIENT) || !clientId.equals(user.getId().value())) {
            return false;
        }

        return photos.values().stream()
                .anyMatch(file -> file.isVisible() && file.getFileName().value().equals(fileName));
    }

    public boolean hasVisibleFile(String fileName) {
        return photos.values().stream()
                .anyMatch(file -> file.isVisible() && file.getFileName().value().equals(fileName));
    }

    public void assignPhotosToAlbum(Map<FileId, File> photos) {
        this.photos = photos;
    }


    private FileName makeUniqueFileName(FileName desired) {

        return FileNamingPolicy.makeUnique(desired,
                candidate -> photos.values().stream().anyMatch(f -> f.getFileName().equals(candidate)));
    }

    private void checkStorage(long maxSizeInGb, long actualSize, long fileSize) {

        long maxSize = calcToGB(maxSizeInGb);

        if (maxSize == 0) return;

        if (actualSize > maxSize) {
            throw new AlbumException("Server storage is full!");
        }

        if (actualSize + fileSize > maxSize) {
            throw new AlbumException("File size too large");
        }

    }

    private boolean canManageFiles(User user) {
        boolean isAdmin = user.getRoles().contains(Role.ADMIN);
        boolean isPhotograph = user.getRoles().contains(Role.PHOTOGRAPHER);

        if (isAdmin) {
            return true;
        }

        return isPhotograph && photographId.equals(user.getId().value());
    }

    public List<File> getFilesByNames(List<FileName> fileNames) {
        Set<String> nameSet = new HashSet<>();
        fileNames.forEach(fn -> nameSet.add(fn.value()));

        return photos.values().stream().filter(file -> nameSet.contains(file.getFileName().value())).toList();
    }

    private boolean isAdminAlbum() {
        return photographId.equals(clientId);
    }

    private void registerEvent(Object event) {
        this.domainEvents.add(event);
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    public long calcToGB(long size) {
        return size * 1024L * 1024L * 1024L;
    }


    public AlbumPath getAlbumPath() {
        return albumPath;
    }

    public AlbumId getAlbumId() {
        return albumId;
    }

    public String getName() {
        return name;
    }

    public UUID getPhotographId() {
        return photographId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public Map<FileId, File> getPhotos() {
        return Collections.unmodifiableMap(photos);
    }

    public Instant getTtd() {
        return ttd;
    }
}
