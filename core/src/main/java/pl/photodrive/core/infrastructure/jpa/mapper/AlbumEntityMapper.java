package pl.photodrive.core.infrastructure.jpa.mapper;

import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.model.File;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.domain.vo.FileId;
import pl.photodrive.core.domain.vo.UserId;
import pl.photodrive.core.infrastructure.jpa.entity.AlbumEntity;
import pl.photodrive.core.infrastructure.jpa.entity.FileEntity;
import pl.photodrive.core.infrastructure.jpa.vo.album.AlbumIdEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.file.FileIdEmbeddable;

import java.util.LinkedHashMap;
import java.util.Map;


public class AlbumEntityMapper {

    public static Album toDomain(AlbumEntity entity) {
        Album album = new Album(new AlbumId(entity.getAlbumId().getValue()),
                entity.getName(),
                entity.getPhotographId(),
                entity.getClientId(),
                entity.getTtd());

        if (entity.getPhotos() != null && !entity.getPhotos().isEmpty()) {
            Map<FileId, File> domainFiles = new LinkedHashMap<>();
            entity.getPhotos().forEach((fileIdEmb, fileEntity) -> {
                var domainFile = FileEntityMapper.toDomain(fileEntity);
                domainFiles.put(new FileId(fileIdEmb.getValue()), domainFile);
            });
            album.assignClient(new UserId(entity.getClientId()));
        }

        return album;
    }

    public static AlbumEntity toEntity(Album album) {
        AlbumEntity entity = AlbumEntity.builder()
                .albumId(new AlbumIdEmbeddable(album.getAlbumId().value()))
                .name(album.getName())
                .photographId(album.getPhotographId())
                .clientId(album.getClientId())
                .ttd(album.getTtd())
                .build();

        if (album.getPhotos() != null && !album.getPhotos().isEmpty()) {
            album.getPhotos().forEach((fileId, domainFile) -> {
                FileEntity fe = FileEntityMapper.toEntity(domainFile);
                fe.setAlbum(entity);
                entity.getPhotos().put(new FileIdEmbeddable(fileId.value()), fe);
            });
        }

        return entity;
    }
}
