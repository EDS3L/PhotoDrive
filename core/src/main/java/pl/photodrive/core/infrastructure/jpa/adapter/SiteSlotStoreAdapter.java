package pl.photodrive.core.infrastructure.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.photodrive.core.application.port.site.SiteSlot;
import pl.photodrive.core.application.port.site.SiteSlotImage;
import pl.photodrive.core.application.port.site.SiteSlotStorePort;
import pl.photodrive.core.application.port.site.SiteSlotVersion;
import pl.photodrive.core.infrastructure.jpa.entity.SiteSlotEntity;
import pl.photodrive.core.infrastructure.jpa.repository.SiteSlotJpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SiteSlotStoreAdapter implements SiteSlotStorePort {

    private final SiteSlotJpaRepository jpa;

    @Override
    public Optional<SiteSlotImage> find(SiteSlot slot) {
        return jpa.findById(slot.name())
                .map(entity -> new SiteSlotImage(entity.getImage(), entity.getUpdatedAt()));
    }

    @Override
    public List<SiteSlotVersion> findVersions() {
        return jpa.findAllProjectedBy().stream()
                .map(view -> new SiteSlotVersion(SiteSlot.valueOf(view.getSlotKey()), view.getUpdatedAt()))
                .toList();
    }

    @Override
    public void put(SiteSlot slot, byte[] image) {
        jpa.save(SiteSlotEntity.builder()
                .slotKey(slot.name())
                .image(image)
                .updatedAt(Instant.now())
                .build());
    }

    @Override
    public void delete(SiteSlot slot) {
        jpa.deleteById(slot.name());
    }
}
