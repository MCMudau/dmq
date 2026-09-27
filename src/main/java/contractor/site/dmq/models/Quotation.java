package contractor.site.dmq.models;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import contractor.site.dmq.enums.QuotationStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A quotation, from the moment a visitor requests it on the landing page (index.html)
 * until an admin has priced it and sent it back.
 *
 * Request fields (client details, service, location, description, budget) come straight
 * from the "Request Your Quotation" form and are required. Pricing fields (totalAmount,
 * validUntil, items, document) stay empty until an admin prepares the quote.
 */
@Entity
@Table(name = "quotation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quotation {

    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String NUMBER_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";   // no 0/O or 1/I lookalikes
    private static final SecureRandom RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long quotationId;

    /** Human-friendly reference shown to the client, e.g. Q-20260927-7KX4. Generated on save. */
    @Column(nullable = false, unique = true, length = 20)
    private String quotationNumber;

    // ------------------------------------------------------------------
    // Requested by the client (index.html quotation form)
    // ------------------------------------------------------------------

    /** "Full Name" field. */
    @Column(nullable = false, length = 100)
    private String clientName;

    /** "Email Address" field. The quote is sent back here. */
    @Column(nullable = false, length = 254)
    private String clientEmail;

    /** "Phone Number" field. */
    @Column(nullable = false, length = 20)
    private String clientPhone;

    /** "Select Service" field: plumbing, renovations, repairs or installation. */
    @Column(nullable = false, length = 50)
    private String serviceType;

    /** "Project Location" field: street address and area. */
    @Column(nullable = false)
    private String location;

    /** "Project Description" field. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** "Estimated Budget" field. Optional free text, e.g. "R5000 - R10000". */
    @Column(length = 100)
    private String estimatedBudget;

    // ------------------------------------------------------------------
    // Filled in by an admin when the quote is prepared
    // ------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuotationStatus status;

    /** Total of all items. Null until the quote has been priced. */
    private Double totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    /** Null until the quote is sent to the client. */
    private LocalDateTime validUntil;

    /** Internal notes for admins; never shown to the client. */
    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

   

    @OneToOne(mappedBy = "quotation", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private QuotationDocument document;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) {
            this.status = QuotationStatus.REQUESTED;
        }
        if (this.currency == null) {
            this.currency = "ZAR";
        }
        if (this.quotationNumber == null) {
            
        }
    }

    
}
