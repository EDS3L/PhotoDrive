package pl.photodrive.core.infrastructure.jpa.mapper;

import pl.photodrive.core.domain.model.Album;
import pl.photodrive.core.domain.vo.AlbumId;
import pl.photodrive.core.infrastructure.jpa.entity.AlbumEntity;
import pl.photodrive.core.infrastructure.jpa.vo.album.AlbumIdEmbeddable;


public class AlbumEntityMapper {

    public static Album toDomain(AlbumEntity entity) {
        return new Album(new AlbumId(entity.getAlbumId().getValue()),
                entity.getName(),
                entity.getPhotographId(),
                entity.getClientId(),
                entity.getTtd());
    }

    public static AlbumEntity toEntity(Album album) {
        return AlbumEntity.builder()
                .albumId(new AlbumIdEmbeddable(album.getAlbumId().value()))
                .name(album.getName())
                .photographId(album.getPhotographId())
                .clientId(album.getClientId())
                .ttd(album.getTtd())
                .build();
    }
}
