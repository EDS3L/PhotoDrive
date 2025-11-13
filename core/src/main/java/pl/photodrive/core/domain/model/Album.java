package pl.photodrive.core.domain.model;

import pl.photodrive.core.domain.event.album.*;
import pl.photodrive.core.domain.exception.AlbumException;
import pl.photodrive.core.domain.vo.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public class Album {

    private final AlbumId albumId;
    private final String name;
    private final UUID photographId;
    private UUID clientId;
    private final Map<FileId, File> photos = new LinkedHashMap<>();
    private final Instant ttd;

    private transient final List<Object> domainEvents = new ArrayList<>();


    public Album(AlbumId albumId, String name, UUID photographId, UUID clientId, Instant ttd) {
        if (name == null) throw new AlbumException("Album name cannot be null!");
        if (photographId == null) throw new AlbumException("Album name cannot be null!");
        this.albumId = albumId;
        this.name = name;
        this.photographId = photographId;
        this.clientId = clientId;
        this.ttd = ttd;
    }

    public static Album createForAdmin(String albumName, User admin) {
        if (!admin.getRoles().contains(Role.ADMIN)) {
            throw new AlbumException("Only administrators can create admin albums");
        }

        Album album = new Album(AlbumId.newId(), albumName, admin.getId().value(), admin.getId().value(), null);
        album.registerEvent(new AdminAlbumCreated(albumName, admin));
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

        Album album = new Album(
                AlbumId.newId(),
                fullAlbumName,
                photographer.getId().value(),
                client.getId().value(),
                null
        );

        album.registerEvent(new PhotographCreateAlbum(photographer, fullAlbumName));
        return album;
    }

    public static String buildClientAlbumName(String baseName, Email clientEmail) {
        return String.format("%s_%s_%s", baseName, clientEmail.value(), LocalDate.now());
    }

    public List<FileAddedResult> addFiles(List<File> files) {
        List<FileAddedResult> results = new ArrayList<>();

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

        photos.put(file.getFileId(), file);

        if (isAdminAlbum()) {
            return new FileAddedResult(file, new FileAddedToAlbum(file.getFileName(), this.name));
        } else {
            return new FileAddedResult(file, new FileAddedToClientAlbum(
                    file.getFileName(),
                    this.name,
                    this.photographId.toString()
            ));
        }
    }

    public void removeFile(FileId fileId) {
        File removedFile = photos.remove(fileId);
        if (removedFile == null) {
            throw new AlbumException("File not found: " + fileId.value());
        }

        registerEvent(new FileRemovedFromAlbum(fileId, this.name));
    }

    public void renameFile(FileId fileId, FileName newFileName) {
        File file = photos.get(fileId);
        if (file == null) {
            throw new AlbumException("File not found: " + fileId.value());
        }

        FileName oldFileName = file.getFileName();
        file.rename(newFileName);

        registerEvent(new FileRenamedInAlbum(fileId, oldFileName, newFileName, this.name));
    }

    public void assignClient(UserId clientId) {
        if (clientId == null) {
            throw new AlbumException("Client ID cannot be null");
        }

        if (this.clientId != null && !this.clientId.equals(clientId.value())) {
            throw new AlbumException("Album already has assigned client");
        }

        this.clientId = clientId.value();
        registerEvent(new ClientAssignedToAlbum(this.albumId, clientId.value()));
    }


    public boolean canAccess(UserId userId, Set<Role> userRoles) {
        if (userRoles.contains(Role.ADMIN)) {
            return true;
        }

        if (userRoles.contains(Role.PHOTOGRAPHER) && photographId.equals(userId.value())) {
            return true;
        }

        return false;
    }

    public boolean containsFile(FileId fileId) {
        return photos.containsKey(fileId);
    }
    public Optional<File> getFile(FileId fileId) {
        return Optional.ofNullable(photos.get(fileId));
    }

    public List<File> getFilesByNames(List<FileName> fileNames) {
        Set<String> nameSet = new HashSet<>();
        fileNames.forEach(fn -> nameSet.add(fn.value()));

        return photos.values().stream()
                .filter(file -> nameSet.contains(file.getFileName().value()))
                .toList();
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
        return photos;
    }

    public Instant getTtd() {
        return ttd;
    }
}
