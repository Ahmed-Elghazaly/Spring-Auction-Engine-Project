package com.bidforge.service;

import com.bidforge.entity.Auction;
import com.bidforge.entity.enums.AuctionStatus;
import com.bidforge.repository.AuctionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

// Spring runs tick() on a background thread, waiting 10 seconds between the end of one run and the start of the next
// race-safe bec the opening/closing methods lock the auction row and recheck its status
// so a scheduler tick racing a manual close doesn't cause an issue

@Service
public class AuctionSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(AuctionSchedulerService.class);

    private final AuctionRepository auctionRepository;
    private final AuctionService auctionService;

    public AuctionSchedulerService(AuctionRepository auctionRepository, AuctionService auctionService) {
        this.auctionRepository = auctionRepository;
        this.auctionService = auctionService;
    }

    @Scheduled(fixedDelay = 10_000)
    public void tick() {
        Instant now = Instant.now();

        for (Auction auction : auctionRepository
                .findByStatusAndStartTimeLessThanEqual(AuctionStatus.SCHEDULED, now)) {
            try {
                auctionService.openBySystem(auction.getId());
                log.info("Auto-opened auction {} ({})", auction.getId(), auction.getTitle());
            } catch (Exception ex) {
                log.error("Failed to auto-open auction {}", auction.getId(), ex);
            }
        }

        for (Auction auction : auctionRepository
                .findByStatusAndEndTimeLessThanEqual(AuctionStatus.OPEN, now)) {
            try {
                auctionService.closeBySystem(auction.getId());
                log.info("Auto-closed auction {} ({})", auction.getId(), auction.getTitle());
            } catch (Exception ex) {
                log.error("Failed to auto-close auction {}", auction.getId(), ex);
            }
        }
    }
}
