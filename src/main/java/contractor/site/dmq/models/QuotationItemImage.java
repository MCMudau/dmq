package contractor.site.dmq.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quotation_item_image")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotationItemImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_item_id", nullable = false)
    private QuotationItem quotationItem;

    @Column(nullable = false)
    private String imageFilename;

    @Column(nullable = false)
    private String imagePath;

    @Column(columnDefinition = "TEXT")
    private String description;
}
