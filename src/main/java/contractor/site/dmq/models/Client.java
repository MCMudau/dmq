package contractor.site.dmq.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "client")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String verificationPin;

    @Column(nullable = false)
    private LocalDateTime pinExpiresAt;

    @Column(nullable = false)
    private Boolean verified;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private Set<Address> addresses = new HashSet<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Project> projects = new HashSet<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Quotation> quotations = new HashSet<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Testimonial> testimonials = new HashSet<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<PreviousProjectComment> comments = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.verified = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
