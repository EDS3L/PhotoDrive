package pl.photodrive.core.infrastructure.jpa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "study_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyProgressEntity {

    @Id
    @Column(length = 64)
    private String codeHash;

    @Column(nullable = false, length = 2000)
    private String learned;

    @Column(nullable = false)
    private Instant updatedAt;
}
