package pl.photodrive.core.infrastructure.jpa.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.photodrive.core.application.port.site.SiteSlot;
import pl.photodrive.core.application.port.site.SiteSlotVersion;
import pl.photodrive.core.infrastructure.jpa.repository.SiteSlotJpaRepository;
import pl.photodrive.core.infrastructure.jpa.repository.SiteSlotJpaRepository.SlotVersionView;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SiteSlotStoreAdapterTest {

    @Mock
    private SiteSlotJpaRepository jpa;

    @InjectMocks
    private SiteSlotStoreAdapter adapter;

    private static SlotVersionView view(String key, Instant updatedAt) {
        return new SlotVersionView() {
            @Override
            public String getSlotKey() {
                return key;
            }

            @Override
            public Instant getUpdatedAt() {
                return updatedAt;
            }
        };
    }

    @Test
    @DisplayName("A database row with a key outside the enum is skipped, so one stale row cannot take down the whole public slot listing")
    void shouldSkipUnknownSlotKeyInsteadOfFailingTheListing() {
        // Given - the table holds a valid slot plus a leftover key no longer in the enum
        Instant now = Instant.now();
        given(jpa.findAllProjectedBy()).willReturn(List.of(
                view("HOME_HERO", now),
                view("REMOVED_LEGACY_SLOT", now)));

        // When
        List<SiteSlotVersion> versions = adapter.findVersions();

        // Then - only the known slot survives; the unknown key neither appears nor throws
        assertThat(versions).extracting(SiteSlotVersion::slot).containsExactly(SiteSlot.HOME_HERO);
    }

    @Test
    @DisplayName("A table full of unknown keys yields an empty listing rather than an exception")
    void shouldReturnEmptyListWhenEveryKeyIsUnknown() {
        // Given - every stored key is outside the current enum
        given(jpa.findAllProjectedBy()).willReturn(List.of(
                view("GHOST_ONE", Instant.now()),
                view("GHOST_TWO", Instant.now())));

        // When / Then - the listing degrades to empty, never blows up
        assertThatCode(() -> assertThat(adapter.findVersions()).isEmpty())
                .doesNotThrowAnyException();
    }
}
