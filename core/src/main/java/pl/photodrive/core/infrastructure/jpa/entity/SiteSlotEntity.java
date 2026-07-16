package pl.photodrive.core.infrastructure.jpa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

// Jeden wiersz = jedno zdjęcie slotu strony wizytówki (klucz = nazwa z enuma SiteSlot).
// MEDIUMBLOB (16 MB) wystarcza z zapasem: serwis przy uploadzie skaluje do 2560 px i zapisuje JPEG.
@Entity
@Table(name = "site_slot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteSlotEntity {

    @Id
    @Column(length = 40)
    private String slotKey;

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] image;

    private Instant updatedAt;
}
