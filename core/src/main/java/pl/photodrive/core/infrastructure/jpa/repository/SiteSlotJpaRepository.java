package pl.photodrive.core.infrastructure.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.photodrive.core.infrastructure.jpa.entity.SiteSlotEntity;

import java.time.Instant;
import java.util.List;

public interface SiteSlotJpaRepository extends JpaRepository<SiteSlotEntity, String> {

    /** Projekcja bez kolumny BLOB — listing wersji jest na gorącej ścieżce strony publicznej. */
    interface SlotVersionView {
        String getSlotKey();

        Instant getUpdatedAt();
    }

    List<SlotVersionView> findAllProjectedBy();
}
