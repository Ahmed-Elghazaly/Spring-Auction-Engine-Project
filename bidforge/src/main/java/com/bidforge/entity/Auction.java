package com.bidforge.entity;

import com.bidforge.entity.enums.AuctionCategory;
import com.bidforge.entity.enums.AuctionStatus;
import com.bidforge.entity.enums.AuctionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Table(name = "AUCTIONS", indexes = {
        // The scheduler constantly asks "which SCHEDULED/OPEN auctions passed their start/end time"
        // the two indices below help speed up those two queries
        @Index(name = "idx_auctions_status_start", columnList = "status, start_time"),
        @Index(name = "idx_auctions_status_end", columnList = "status, end_time"),
        // this index speeds up searching auctions by seller_id
        @Index(name = "idx_auctions_seller", columnList = "seller_id")})
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionCategory category;

    // ENGLISH or SEALED_BID , doesn't change after creation (no setter)
    @Enumerated(EnumType.STRING)
    @Column(name = "auction_type", nullable = false, updatable = false, length = 20)
    private AuctionType auctionType;

    // Default on creation is SCHEDULED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionStatus status = AuctionStatus.SCHEDULED;

    @Column(name = "starting_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal startingPrice;

    @Column(name = "min_increment", precision = 19, scale = 2)
    private BigDecimal minIncrement;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "current_highest_bid", precision = 19, scale = 2)
    private BigDecimal currentHighestBid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        // seed data can set its own creation time for testing
        // normal creation always passes through here with null and gets the real current time
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = this.createdAt;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Used only by seed data
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    protected Auction() {
    }

    public Auction(String title, String description, AuctionCategory category, AuctionType auctionType, BigDecimal startingPrice, BigDecimal minIncrement, Instant startTime, Instant endTime, User seller) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.auctionType = auctionType;
        this.startingPrice = startingPrice;
        this.minIncrement = minIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
        this.seller = seller;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AuctionCategory getCategory() {
        return category;
    }

    public void setCategory(AuctionCategory category) {
        this.category = category;
    }

    public AuctionType getAuctionType() {
        return auctionType;
    }


    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    public BigDecimal getMinIncrement() {
        return minIncrement;
    }

    public void setMinIncrement(BigDecimal minIncrement) {
        this.minIncrement = minIncrement;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getCurrentHighestBid() {
        return currentHighestBid;
    }

    public void setCurrentHighestBid(BigDecimal currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    public User getSeller() {
        return seller;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
