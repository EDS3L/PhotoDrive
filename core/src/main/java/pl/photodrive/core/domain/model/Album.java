package pl.photodrive.core.domain.model;


import pl.photodrive.core.domain.exception.AlbumException;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.domain.vo.FileName;
import pl.photodrive.core.domain.vo.UserId;

import java.time.Instant;
import java.util.*;

public class Album {

    private final AlbumId albumId;
    private final String name;
    private final UUID photographId;
    private UUID clientId;
    private final Map<FileId, File> photos = new LinkedHashMap<>();
    private final Instant ttd;

    public Album(AlbumId albumId, String name, UUID photographId, UUID clientId, Instant ttd) {
        if(name == null) throw new AlbumException("Album name cannot be null!");
        if(photographId == null) throw new AlbumException("Album name cannot be null!");
        if(ttd == null) throw new AlbumException("TTD name cannot be null!");
        this.albumId = albumId;
        this.name = name;
        this.photographId = photographId;
        this.clientId = clientId;
        this.ttd = ttd;
    }

    public static Album create(String name, UUID photographId, UUID clientId, Instant ttd) {
        return new Album(new AlbumId(AlbumId.newId()),name,photographId,clientId, ttd);
    }

    public void addClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public void addFile(File file) {
        this.photos.putIfAbsent(file.getFileId(), file);
    }

    public void addFiles(Map<FileId, File> newPhotos) {
        //todo sprawdzic czy ten wyjatek jest potrzebny
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
        if(file == null) throw new AlbumException("File not found!");
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
