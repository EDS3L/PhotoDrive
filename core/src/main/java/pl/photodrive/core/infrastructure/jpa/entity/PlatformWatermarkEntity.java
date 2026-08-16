package pl.photodrive.core.infrastructure.jpa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "platform_watermark")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformWatermarkEntity {

    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id;

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] image;

    private Instant updatedAt;
}
