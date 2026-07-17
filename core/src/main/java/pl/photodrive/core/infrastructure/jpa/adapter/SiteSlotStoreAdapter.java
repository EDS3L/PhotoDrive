package pl.photodrive.core.infrastructure.jpa.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import pl.photodrive.core.application.port.site.SiteSlot;
import pl.photodrive.core.application.port.site.SiteSlotImage;
import pl.photodrive.core.application.port.site.SiteSlotStorePort;
import pl.photodrive.core.application.port.site.SiteSlotVersion;
import pl.photodrive.core.infrastructure.jpa.entity.SiteSlotEntity;
import pl.photodrive.core.infrastructure.jpa.repository.SiteSlotJpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
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
        // Klucze przychodzą Z BAZY — wiersz z kluczem spoza enuma (przemianowana wartość,
        // ręczna edycja) nie może wywalić CAŁEGO publicznego listingu (500 na stronie głównej).
        // Nieznany klucz pomijamy z ostrzeżeniem zamiast rzucać.
        return jpa.findAllProjectedBy().stream()
                .map(view -> parseSlot(view.getSlotKey())
                        .map(slot -> new SiteSlotVersion(slot, view.getUpdatedAt()))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private Optional<SiteSlot> parseSlot(String key) {
        try {
            return Optional.of(SiteSlot.valueOf(key));
        } catch (IllegalArgumentException e) {
            log.warn("Ignoring unknown site slot key from database: {}", key);
            return Optional.empty();
        }
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
