package com.bidforge.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;


// one bid on one auction, insert only, never updated or deleted
// there is NO unique constraint on (auction, bidder) because English auctions allow many bids per user
// the one-sealed-bid-per-user rule is enforced in the service layer

@Entity
@Table(name = "BIDS", indexes = {
        // Winner determination relies on highest amount first then earliest bid on ties
        @Index(name = "idx_bids_auction_amount", columnList = "auction_id, amount DESC, created_at ASC"),
        // speed up searching of bids by specific user_id
        @Index(name = "idx_bids_bidder", columnList = "bidder_id")
})
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bidder_id", nullable = false)
    private User bidder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {

        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    // Used only by seed data
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    protected Bid() {
    }

    public Bid(BigDecimal amount, Auction auction, User bidder) {
        this.amount = amount;
        this.auction = auction;
        this.bidder = bidder;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Auction getAuction() {
        return auction;
    }

    public User getBidder() {
        return bidder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
