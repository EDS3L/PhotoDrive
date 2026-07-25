package pl.photodrive.core.infrastructure.jpa.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.photodrive.core.application.port.file.PlatformWatermark;
import pl.photodrive.core.infrastructure.jpa.entity.PlatformWatermarkEntity;
import pl.photodrive.core.infrastructure.jpa.repository.PlatformWatermarkJpaRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * Znak wodny platformy jest <b>singletonem</b> w bazie — jeden wiersz o stałym id.
 * Gdyby zapis używał nowego id, każde wgranie loga dodawałoby wiersz, a odczyt po
 * stałym id nadal zwracałby STARE logo (albo nic).
 */
@ExtendWith(MockitoExtension.class)
class WatermarkStoreAdapterTest {

    @Mock
    private PlatformWatermarkJpaRepository jpa;

    @InjectMocks
    private WatermarkStoreAdapter adapter;

    @Test
    @DisplayName("Uploading a logo writes the singleton row, so a replacement overwrites the old logo instead of piling up rows the reader never sees")
    void shouldStoreWatermarkUnderTheSingletonId() {
        // Given
        byte[] image = {1, 2, 3, 4};

        // When
        adapter.put(image);

        // Then
        ArgumentCaptor<PlatformWatermarkEntity> saved = ArgumentCaptor.forClass(PlatformWatermarkEntity.class);
        then(jpa).should().save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(PlatformWatermarkEntity.SINGLETON_ID);
        assertThat(saved.getValue().getImage()).isEqualTo(image);
    }

    @Test
    @DisplayName("Uploading a logo stamps the update time, because that timestamp is the cache-busting version in every watermarked photo URL")
    void shouldStampUpdatedAtOnUpload() {
        // Given
        Instant before = Instant.now();

        // When
        adapter.put(new byte[]{9});

        // Then - a missing timestamp would freeze the version and clients would keep
        // seeing photos watermarked with the previous logo
        ArgumentCaptor<PlatformWatermarkEntity> saved = ArgumentCaptor.forClass(PlatformWatermarkEntity.class);
        then(jpa).should().save(saved.capture());
        assertThat(saved.getValue().getUpdatedAt()).isNotNull().isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("Reading the watermark exposes the stored image with its version, so the cache key can follow the logo")
    void shouldReturnStoredWatermarkWithVersion() {
        // Given
        Instant updatedAt = Instant.parse("2026-07-25T10:00:00Z");
        given(jpa.findById(PlatformWatermarkEntity.SINGLETON_ID)).willReturn(Optional.of(
                PlatformWatermarkEntity.builder()
                        .id(PlatformWatermarkEntity.SINGLETON_ID)
                        .image(new byte[]{7, 7})
                        .updatedAt(updatedAt)
                        .build()));

        // When
        Optional<PlatformWatermark> watermark = adapter.get();

        // Then
        assertThat(watermark).isPresent();
        assertThat(watermark.get().image()).isEqualTo(new byte[]{7, 7});
        assertThat(watermark.get().updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("No configured logo reads as empty rather than as a blank watermark, which is what hides the option from photographers")
    void shouldReturnEmptyWhenNoWatermarkConfigured() {
        // Given
        given(jpa.findById(PlatformWatermarkEntity.SINGLETON_ID)).willReturn(Optional.empty());

        // When / Then
        assertThat(adapter.get()).isEmpty();
    }
}
