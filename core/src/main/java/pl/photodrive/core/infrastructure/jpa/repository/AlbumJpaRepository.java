package pl.photodrive.core.infrastructure.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.photodrive.core.infrastructure.jpa.entity.AlbumEntity;
import pl.photodrive.core.infrastructure.jpa.vo.album.AlbumIdEmbeddable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlbumJpaRepository extends JpaRepository<AlbumEntity, AlbumIdEmbeddable> {

    AlbumEntity findByPhotographId(UUID photographId);

    Optional<AlbumEntity> findByAlbumIdAndIsPublicTrue(AlbumIdEmbeddable albumId);

    List<AlbumEntity> findByClientId(UUID clientId);

    List<AlbumEntity> findAllByPhotographId(UUID photographId);

    List<AlbumEntity> findAllByClientId(UUID clientId);

    List<AlbumEntity> findByTtdBeforeAndTtdIsNotNull(Instant now);

    Optional<AlbumEntity> findByName(String name);

    boolean existsByName(String name);

    Optional<AlbumEntity> findByNameAndIsPublicTrue(String name);

    /** Wiersz publicznego listingu — bez kolekcji plików; widoczne zdjęcia liczy baza (B.35). */
    interface PublicAlbumSummaryView {
        UUID getAlbumId();

        String getName();

        String getDisplayName();

        Integer getDisplayOrder();

        Long getVisibleCount();
    }

    @Query("""
            select a.albumId.value as albumId,
                   a.name as name,
                   a.displayName as displayName,
                   a.displayOrder as displayOrder,
                   (select count(f) from FileEntity f where f.album = a and f.isVisible = true) as visibleCount
            from AlbumEntity a
            where a.isPublic = true
            """)
    List<PublicAlbumSummaryView> findPublicAlbumSummaries();
}
