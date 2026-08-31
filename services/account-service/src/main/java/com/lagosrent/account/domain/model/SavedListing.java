package com.lagosrent.account.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saved_listings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"account_id", "listing_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class SavedListing {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public SavedListing(Account account, UUID listingId) {
        this.account = account;
        this.listingId = listingId;
        this.createdAt = Instant.now();
    }
}
