package pl.photodrive.core.infrastructure.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.photodrive.core.application.port.file.PlatformWatermark;
import pl.photodrive.core.application.port.file.WatermarkStorePort;
import pl.photodrive.core.infrastructure.jpa.entity.PlatformWatermarkEntity;
import pl.photodrive.core.infrastructure.jpa.repository.PlatformWatermarkJpaRepository;

import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WatermarkStoreAdapter implements WatermarkStorePort {

    private final PlatformWatermarkJpaRepository jpa;

    @Override
    public Optional<PlatformWatermark> get() {
        return jpa.findById(PlatformWatermarkEntity.SINGLETON_ID)
                .map(entity -> new PlatformWatermark(entity.getImage(), entity.getUpdatedAt()));
    }

    @Override
    public void put(byte[] image) {
        jpa.save(PlatformWatermarkEntity.builder()
                .id(PlatformWatermarkEntity.SINGLETON_ID)
                .image(image)
                .updatedAt(Instant.now())
                .build());
    }

    @Override
    public void delete() {
        jpa.deleteById(PlatformWatermarkEntity.SINGLETON_ID);
    }

    @Override
    public boolean exists() {
        return jpa.existsById(PlatformWatermarkEntity.SINGLETON_ID);
    }
}
