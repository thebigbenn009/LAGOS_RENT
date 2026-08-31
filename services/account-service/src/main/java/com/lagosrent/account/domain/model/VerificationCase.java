package com.lagosrent.account.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_case")
@Getter
@Setter
@NoArgsConstructor
public class VerificationCase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_profile_id", nullable = false)
    private LandlordProfile landlordProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Column(name = "document_s3_key", nullable = false)
    private String documentS3Key;

    @Column(name = "document_filename", nullable = false)
    private String documentFilename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationCaseStatus status = VerificationCaseStatus.PENDING;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt = Instant.now();

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Version
    private Long version;

    public VerificationCase(LandlordProfile landlordProfile,
                            DocumentType documentType, String documentS3Key,
                            String documentFilename) {
        this.landlordProfile = landlordProfile;
        this.documentType = documentType;
        this.documentS3Key = documentS3Key;
        this.documentFilename = documentFilename;
        this.submittedAt = Instant.now();

    }
}
