package pl.photodrive.core.infrastructure.jpa.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.photodrive.core.infrastructure.jpa.entity.FileEntity;
import pl.photodrive.core.infrastructure.jpa.vo.album.AlbumIdEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.file.FileIdEmbeddable;
import pl.photodrive.core.infrastructure.jpa.vo.file.FileNameEmbeddable;

public interface FileJpaRepository extends JpaRepository<FileEntity, FileIdEmbeddable> {

    @Query("""
            SELECT CASE WHEN COUNT(f) > 0
            THEN true ELSE false END
            FROM FileEntity f WHERE f.album.albumId = :albumId AND f.fileName = :fileName
            """)
    boolean existsByAlbumIdAndFileName(@Param("albumId") AlbumIdEmbeddable albumId, @Param("fileName") FileNameEmbeddable fileName);

}
