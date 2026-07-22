package com.bidforge.repository;

import com.bidforge.entity.Auction;
import com.bidforge.entity.enums.AuctionCategory;
import com.bidforge.entity.enums.AuctionStatus;
import com.bidforge.entity.enums.AuctionType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {


    // Loads the auction and locks its row until the current transaction ends
    // any state changing operation like placing a bid, opening, closing, cancelling goes through this method
    // two simultaneous requests on the same auction are forced to an order
    // the second waits a bit, then sees the first one's committed changes and is validated against the new state

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Auction a where a.id = :id")
    Optional<Auction> findByIdForUpdate(@Param("id") Long id);


    // browsing query
    @Query("""
            select a from Auction a
            where (:status is null or a.status = :status)
              and (:type is null or a.auctionType = :type)
              and (:category is null or a.category = :category)
              and (:q is null or lower(a.title) like lower(concat('%', :q, '%')))
            """)
    Page<Auction> search(@Param("status") AuctionStatus status,
                         @Param("type") AuctionType type,
                         @Param("category") AuctionCategory category,
                         @Param("q") String q,
                         Pageable pageable);


    // Auctions created by a given user
    // used for getting a logged in user's auctions "/api/auctions/mine"
    Page<Auction> findBySellerUsername(String username, Pageable pageable);


    // admin version of search, adds a seller filter
    @Query("""
            select a from Auction a
            where (:status is null or a.status = :status)
              and (:type is null or a.auctionType = :type)
              and (:category is null or a.category = :category)
              and (:q is null or lower(a.title) like lower(concat('%', :q, '%')))
              and (:seller is null or a.seller.username = :seller)
            """)
    Page<Auction> adminSearch(@Param("status") AuctionStatus status,
                              @Param("type") AuctionType type,
                              @Param("category") AuctionCategory category,
                              @Param("q") String q,
                              @Param("seller") String seller,
                              Pageable pageable);

    // the two methods below are used by the scheduler (backed by index idx_auctions_status_end) --- */

    // SCHEDULED auctions whose start time has arrived, should auto open
    List<Auction> findByStatusAndStartTimeLessThanEqual(AuctionStatus status, Instant time);


    // OPEN auctions whose end time has passed, should auto close
    List<Auction> findByStatusAndEndTimeLessThanEqual(AuctionStatus status, Instant time);
}
