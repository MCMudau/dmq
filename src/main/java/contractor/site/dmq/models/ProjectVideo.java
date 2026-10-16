package contractor.site.dmq.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * A video belonging to a project. Deliberately one-directional: Project knows nothing
 * about videos, so the video feature can be added or removed without touching it.
 * ON DELETE CASCADE lets the database drop a project's video rows when the project goes.
 */
@Entity
@Table(name = "project_video")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** A video belongs to exactly one project. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Project project;

    /** Public link to the video, e.g. /uploads/videos/projects/3/ab12.mp4 */
    @Column(nullable = false, length = 500)
    private String videoUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}
