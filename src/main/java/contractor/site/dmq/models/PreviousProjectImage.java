package contractor.site.dmq.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "previous_project_image")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreviousProjectImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_project_id", nullable = false)
    private PreviousProject previousProject;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private Boolean coverImage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
        if (this.coverImage == null) {
            this.coverImage = false;
        }
    }
}
