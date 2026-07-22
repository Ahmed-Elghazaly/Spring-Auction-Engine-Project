package com.bidforge.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;


// created exactly once, at the moment the auction closes, and never modified afterwards
// only for CLOSED auctions
// winner, winningBid, finalPrice fields are all NULL when the auction closes with 0 bids

@Entity
@Table(name = "AUCTION_RESULTS")
public class AuctionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auction_id", nullable = false, unique = true)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winning_bid_id")
    private Bid winningBid;

    @Column(name = "final_price", precision = 19, scale = 2)
    private BigDecimal finalPrice;

    @Column(name = "closed_at", nullable = false)
    private Instant closedAt;

    protected AuctionResult() {
    }

    public AuctionResult(Auction auction, User winner, Bid winningBid, BigDecimal finalPrice, Instant closedAt) {
        this.auction = auction;
        this.winner = winner;
        this.winningBid = winningBid;
        this.finalPrice = finalPrice;
        this.closedAt = closedAt;
    }

    public Long getId() {
        return id;
    }

    public Auction getAuction() {
        return auction;
    }

    public User getWinner() {
        return winner;
    }

    public Bid getWinningBid() {
        return winningBid;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public Instant getClosedAt() {
        return closedAt;
    }
}
