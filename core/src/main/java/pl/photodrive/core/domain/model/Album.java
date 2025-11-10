package pl.photodrive.core.domain.model;


import pl.photodrive.core.domain.event.AdminAlbumCreated;
import pl.photodrive.core.domain.event.PhotographCreateAlbum;
import pl.photodrive.core.domain.event.PhotographerRootAlbumCreated;
import pl.photodrive.core.domain.exception.AlbumException;
import pl.photodrive.core.domain.port.repository.AlbumRepository;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.domain.vo.FileName;

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

    public void registerEvent(Object event) {
        this.domainEvents.add(event);
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    public Album(AlbumId albumId, String name, UUID photographId, UUID clientId, Instant ttd) {
        if (name == null) throw new AlbumException("Album name cannot be null!");
        if (photographId == null) throw new AlbumException("Album name cannot be null!");
        this.albumId = albumId;
        this.name = name;
        this.photographId = photographId;
        this.clientId = clientId;
        this.ttd = ttd;
    }

    public static Album createForAdmin(String albumName, User admin, AlbumRepository albumRepository) {
        checkIfCreatorIsAnAdmin(admin);
        String name = createAlbumName(albumName, admin.getEmail().value());
        checkIfAlbumExists(albumRepository,name);
        Album album = new Album(AlbumId.newId(), name, admin.getId().value(), admin.getId().value(), null);
        album.registerEvent(new AdminAlbumCreated(albumName,admin));
        return album;
    }

    public static Album createForClient(String albumName, User photographer, User client, AlbumRepository albumRepository) {
        checkIfCreatorIsAnPhotograph(photographer);

        if (!client.getRoles().contains(Role.CLIENT)) {
            throw new AlbumException("Album must be assigned to a client.");
        }

        String name = createAlbumName(albumName, client.getEmail().value());
        checkIfAlbumExists(albumRepository,name);
        Album album = new Album(AlbumId.newId(), name, photographer.getId().value(), client.getId().value(), null);
        album.registerEvent(new PhotographCreateAlbum(photographer,name));

        return album;
    }

    public static Album createPhotographerRootAlbum(User photographer) {
        checkIfCreatorIsAnPhotograph(photographer);

        String name = photographer.getEmail().value();

        Album album = new Album(AlbumId.newId(), name, photographer.getId().value(), null, null);
        album.registerEvent(new PhotographerRootAlbumCreated(
                album.getAlbumId(),
                photographer.getId(),
                photographer.getEmail()
        ));
        return album;

    }

    private static void checkIfCreatorIsAnAdmin(User admin) {
        if (!admin.getRoles().contains(Role.ADMIN)) {
            throw new AlbumException("Creator must be a admin.");
        }
    }

    private static void checkIfCreatorIsAnPhotograph(User photographer) {
        if (!photographer.getRoles().contains(Role.PHOTOGRAPHER)) {
            throw new AlbumException("Creator must be a photographer.");
        }
    }

    private static void checkIfAlbumExists(AlbumRepository albumRepository, String name) {
        if (albumRepository.existsByName(name)) throw new AlbumException("Folder already exists!");

    }

    private static String createAlbumName(String albumName, String email) {
        return albumName + "_" + email + "_" + LocalDate.now();
    }

    public void addClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public void addFile(File file) {
        this.photos.putIfAbsent(file.getFileId(), file);
    }

    public void addFiles(Map<FileId, File> newPhotos) {
        if (newPhotos == null || newPhotos.isEmpty()) throw new AlbumException("There isn't a file to add");

        for (Map.Entry<FileId, File> entry : newPhotos.entrySet()) {
            this.photos.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    public void removeFile(FileId fileId) {
        this.photos.remove(fileId);
    }

    public void renameFile(FileId fileId, FileName newFileName) {
        File file = photos.get(fileId);
        if (file == null) throw new AlbumException("File not found!");
        file.rename(newFileName);
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
