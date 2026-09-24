package contractor.site.dmq.models;

import contractor.site.dmq.enums.QuotationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "quotation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long quotationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false, unique = true)
    private String quotationNumber;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuotationStatus status;

    @Column(nullable = false)
    private LocalDateTime validUntil;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private Set<QuotationItem> items = new HashSet<>();

    @OneToOne(mappedBy = "quotation", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private QuotationDocument document;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = QuotationStatus.DRAFT;
        }
        if (this.currency == null) {
            this.currency = "ZAR";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
