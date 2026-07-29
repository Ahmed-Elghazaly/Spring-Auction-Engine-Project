package com.bidforge.service;

import com.bidforge.AbstractIntegrationTest;
import com.bidforge.dto.request.PlaceBidRequest;
import com.bidforge.entity.Auction;
import com.bidforge.entity.User;
import com.bidforge.entity.enums.AuctionCategory;
import com.bidforge.entity.enums.AuctionStatus;
import com.bidforge.entity.enums.AuctionType;
import com.bidforge.entity.enums.RoleName;
import com.bidforge.exception.ApiException;
import com.bidforge.repository.AuctionRepository;
import com.bidforge.repository.BidRepository;
import com.bidforge.repository.RoleRepository;
import com.bidforge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class BiddingConcurrencyIT extends AbstractIntegrationTest {

    @Autowired
    private BidService bidService;
    @Autowired
    private AuctionRepository auctionRepository;
    @Autowired
    private BidRepository bidRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

    private String sellerName;
    private String firstBidderName;
    private String secondBidderName;
    private Long auctionId;


    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        sellerName = "seller_" + unique;
        firstBidderName = "bidder1_" + unique;
        secondBidderName = "bidder2_" + unique;

        User seller = createUser(sellerName);
        createUser(firstBidderName);
        createUser(secondBidderName);

        Auction auction = new Auction("Concurrency test auction", "Two bidders will hit this at the same instant.", AuctionCategory.OTHER, AuctionType.ENGLISH, new BigDecimal("100.00"), new BigDecimal("10.00"), Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(1, ChronoUnit.DAYS), seller);
        auction.setStatus(AuctionStatus.OPEN);
        auctionRepository.save(auction);

        this.auctionId = auction.getId();
    }

    @Test
    void twoSimultaneousIdenticalBids_onlyOneIsAccepted() throws Exception {

        List<String> outcomes = placeConcurrentBids(List.of(firstBidderName, secondBidderName), new BigDecimal("100.00"));

        assertThat(outcomes).as("exactly one of two simultaneous equal bids may be accepted").containsExactlyInAnyOrder("ACCEPTED", "REJECTED");

        assertThat(bidRepository.findByAuctionId(auctionId, PageRequest.of(0, 10)).getTotalElements()).as("only the accepted bid may be persisted").isEqualTo(1);

        assertThat(auctionRepository.findById(auctionId).orElseThrow().getCurrentHighestBid()).as("the denormalised highest bid must match the single accepted bid").isEqualByComparingTo("100.00");
    }

    @Test
    void manySimultaneousBids_leaveTheAuctionConsistent() throws Exception {

        List<String> bidders = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String name = "rush_" + UUID.randomUUID().toString().substring(0, 8);
            createUser(name);
            bidders.add(name);
        }

        List<String> outcomes = placeConcurrentBids(bidders, new BigDecimal("100.00"));

        assertThat(outcomes.stream().filter("ACCEPTED"::equals).count()).as("the lock must serialise all five attempts down to one winner").isEqualTo(1);

        assertThat(bidRepository.findByAuctionId(auctionId, PageRequest.of(0, 20)).getTotalElements()).isEqualTo(1);
    }

    @Test
    void sameUserBiddingTwiceAtOnceOnSealedAuction_isStillOnlyOneBid() throws Exception {

        User seller = userRepository.findByUsername(sellerName).orElseThrow();
        Auction sealed = new Auction("Sealed concurrency test", "One bidder, two simultaneous attempts.", AuctionCategory.OTHER, AuctionType.SEALED_BID, new BigDecimal("50.00"), null, Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(1, ChronoUnit.DAYS), seller);
        sealed.setStatus(AuctionStatus.OPEN);
        auctionRepository.save(sealed);

        List<String> outcomes = placeConcurrentBids(sealed.getId(), List.of(firstBidderName, firstBidderName), new BigDecimal("75.00"));

        assertThat(outcomes.stream().filter("ACCEPTED"::equals).count()).as("a user may only ever have one sealed bid, even under a race").isEqualTo(1);

        assertThat(bidRepository.existsByAuctionIdAndBidderUsername(sealed.getId(), firstBidderName)).isTrue();
    }


    private List<String> placeConcurrentBids(List<String> bidders, BigDecimal amount) throws Exception {
        return placeConcurrentBids(this.auctionId, bidders, amount);
    }

    private List<String> placeConcurrentBids(Long targetAuctionId, List<String> bidders, BigDecimal amount) throws Exception {

        int threadCount = bidders.size();
        CyclicBarrier startTogether = new CyclicBarrier(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        try {
            List<Future<String>> futures = new ArrayList<>();
            for (String bidder : bidders) {
                Callable<String> attempt = () -> {
                    startTogether.await(10, TimeUnit.SECONDS);
                    try {
                        bidService.placeBid(bidder, targetAuctionId, new PlaceBidRequest(amount));
                        return "ACCEPTED";
                    } catch (ApiException expected) {

                        return "REJECTED";
                    }
                };
                futures.add(executor.submit(attempt));
            }

            List<String> outcomes = new ArrayList<>();
            for (Future<String> future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            executor.shutdownNow();
        }
    }

    private User createUser(String username) {
        User user = new User(username, username + "@example.test", "$2a$10$notARealHashOnlyUsedForTests............................", "Test", "User");
        user.addRole(roleRepository.findByName(RoleName.ROLE_USER).orElseThrow());
        return userRepository.save(user);
    }
}
