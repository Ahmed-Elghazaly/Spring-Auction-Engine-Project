package com.bidforge.service;

import com.bidforge.dto.request.PlaceBidRequest;
import com.bidforge.dto.response.BidResponse;
import com.bidforge.dto.response.MyBidResponse;
import com.bidforge.entity.Auction;
import com.bidforge.entity.Bid;
import com.bidforge.entity.User;
import com.bidforge.entity.enums.AuctionCategory;
import com.bidforge.entity.enums.AuctionStatus;
import com.bidforge.entity.enums.AuctionType;
import com.bidforge.exception.BidTooLowException;
import com.bidforge.exception.DuplicateSealedBidException;
import com.bidforge.exception.InvalidAuctionStateException;
import com.bidforge.exception.SellerCannotBidException;
import com.bidforge.repository.AuctionRepository;
import com.bidforge.repository.BidRepository;
import com.bidforge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    @Mock
    private BidRepository bidRepository;
    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditService auditService;

    private BidService service;

    private User seller;
    private User bidder;
    private final Pageable page = PageRequest.of(0, 20);

    @BeforeEach
    void setUp() {
        service = new BidService(bidRepository, auctionRepository, userRepository, auditService);
        seller = user(1L, "sara");
        bidder = user(2L, "layla");
        lenient().when(userRepository.findByUsername("sara")).thenReturn(Optional.of(seller));
        lenient().when(userRepository.findByUsername("layla")).thenReturn(Optional.of(bidder));
        lenient().when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> {
            Bid bid = inv.getArgument(0);
            ReflectionTestUtils.setField(bid, "id", 99L);
            return bid;
        });
    }


    @Test
    void sellerCannotBidOnOwnAuction() {
        lockReturns(englishAuction(AuctionStatus.OPEN, null));
        assertThatThrownBy(() -> service.placeBid("sara", 10L, bid("150.00")))
                .isInstanceOf(SellerCannotBidException.class);
    }

    @Test
    void rejectsBidWhenAuctionNotOpen() {
        lockReturns(englishAuction(AuctionStatus.SCHEDULED, null));
        assertThatThrownBy(() -> service.placeBid("layla", 10L, bid("150.00")))
                .isInstanceOf(InvalidAuctionStateException.class)
                .hasMessageContaining("SCHEDULED");
    }

    @Test
    void rejectsBidAfterEndTimePassed() {
        Auction auction = englishAuction(AuctionStatus.OPEN, null);
        auction.setEndTime(Instant.now().minus(1, ChronoUnit.MINUTES));
        lockReturns(auction);
        assertThatThrownBy(() -> service.placeBid("layla", 10L, bid("150.00")))
                .isInstanceOf(InvalidAuctionStateException.class)
                .hasMessageContaining("end time");
    }


    @Test
    void english_firstBidMustReachStartingPrice() {
        lockReturns(englishAuction(AuctionStatus.OPEN, null));
        assertThatThrownBy(() -> service.placeBid("layla", 10L, bid("99.99")))
                .isInstanceOf(BidTooLowException.class)
                .hasMessageContaining("starting price");
    }

    @Test
    void english_firstBidAtStartingPriceAccepted_andBecomesHighest() {
        Auction auction = lockReturns(englishAuction(AuctionStatus.OPEN, null));
        BidResponse response = service.placeBid("layla", 10L, bid("100.00"));

        assertThat(response.amount()).isEqualByComparingTo("100.00");
        assertThat(auction.getCurrentHighestBid()).isEqualByComparingTo("100.00");
        verify(bidRepository).save(any(Bid.class));
    }

    @Test
    void english_mustBeatHighestByIncrement() {
        lockReturns(englishAuction(AuctionStatus.OPEN, new BigDecimal("100.00")));
        assertThatThrownBy(() -> service.placeBid("layla", 10L, bid("105.00")))
                .isInstanceOf(BidTooLowException.class)
                .hasMessageContaining("110");
    }

    @Test
    void english_validOutbidUpdatesHighest() {
        Auction auction = lockReturns(englishAuction(AuctionStatus.OPEN, new BigDecimal("100.00")));
        service.placeBid("layla", 10L, bid("110.00"));
        assertThat(auction.getCurrentHighestBid()).isEqualByComparingTo("110.00");
    }



    @Test
    void sealed_secondBidBySameUserRejected() {
        lockReturns(sealedAuction(AuctionStatus.OPEN));
        when(bidRepository.existsByAuctionIdAndBidderUsername(10L, "layla")).thenReturn(true);
        assertThatThrownBy(() -> service.placeBid("layla", 10L, bid("500.00")))
                .isInstanceOf(DuplicateSealedBidException.class);
        verify(bidRepository, never()).save(any());
    }

    @Test
    void sealed_bidMustReachStartingPrice() {
        lockReturns(sealedAuction(AuctionStatus.OPEN));
        when(bidRepository.existsByAuctionIdAndBidderUsername(10L, "layla")).thenReturn(false);
        assertThatThrownBy(() -> service.placeBid("layla", 10L, bid("99.00")))
                .isInstanceOf(BidTooLowException.class);
    }

    @Test
    void sealed_acceptedBidNeverTouchesCurrentHighest() {
        Auction auction = lockReturns(sealedAuction(AuctionStatus.OPEN));
        when(bidRepository.existsByAuctionIdAndBidderUsername(10L, "layla")).thenReturn(false);

        service.placeBid("layla", 10L, bid("500.00"));

        assertThat(auction.getCurrentHighestBid()).isNull();
    }



    @Test
    void english_bidListIsPublic() {
        findReturns(englishAuction(AuctionStatus.OPEN, new BigDecimal("100.00")));
        when(bidRepository.findByAuctionId(10L, page)).thenReturn(pageOfOneBid());

        Page<BidResponse> result = service.getBidsForAuction(null, 10L, page);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void sealed_open_anonymousSeesNothing() {
        findReturns(sealedAuction(AuctionStatus.OPEN));
        Page<BidResponse> result = service.getBidsForAuction(null, 10L, page);

        assertThat(result.getTotalElements()).isZero();
        verify(bidRepository, never()).findByAuctionId(anyLong(), any());
    }

    @Test
    void sealed_open_viewerSeesOnlyOwnBids() {
        findReturns(sealedAuction(AuctionStatus.OPEN));
        when(bidRepository.findByAuctionIdAndBidderUsername(10L, "layla", page))
                .thenReturn(pageOfOneBid());

        Page<BidResponse> result = service.getBidsForAuction("layla", 10L, page);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(bidRepository, never()).findByAuctionId(anyLong(), any());
    }

    @Test
    void sealed_closed_everythingRevealed() {
        findReturns(sealedAuction(AuctionStatus.CLOSED));
        when(bidRepository.findByAuctionId(10L, page)).thenReturn(pageOfOneBid());

        Page<BidResponse> result = service.getBidsForAuction(null, 10L, page);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }


    @Test
    void myBids_flagsWinningAndOutbidCorrectly() {
        Auction auction = englishAuction(AuctionStatus.OPEN, new BigDecimal("110.00"));
        Bid winning = new Bid(new BigDecimal("110.00"), auction, bidder);
        Bid outbid = new Bid(new BigDecimal("100.00"), auction, bidder);
        when(bidRepository.findByBidderUsername("layla", page))
                .thenReturn(new PageImpl<>(List.of(winning, outbid), page, 2));

        Page<MyBidResponse> result = service.myBids("layla", page);

        assertThat(result.getContent().get(0).currentlyWinning()).isTrue();
        assertThat(result.getContent().get(1).currentlyWinning()).isFalse();
    }

    @Test
    void myBids_sealedBidsHaveNoWinningFlag() {
        Auction auction = sealedAuction(AuctionStatus.OPEN);
        Bid sealedBid = new Bid(new BigDecimal("500.00"), auction, bidder);
        when(bidRepository.findByBidderUsername("layla", page))
                .thenReturn(new PageImpl<>(List.of(sealedBid), page, 1));

        Page<MyBidResponse> result = service.myBids("layla", page);

        assertThat(result.getContent().get(0).currentlyWinning()).isNull();
    }


    private User user(Long id, String username) {
        User user = new User(username, username + "@x.com", "hash", "F", "L");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Auction englishAuction(AuctionStatus status, BigDecimal currentHighest) {
        Auction auction = new Auction("English", "d", AuctionCategory.OTHER, AuctionType.ENGLISH,
                new BigDecimal("100.00"), new BigDecimal("10.00"),
                Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(1, ChronoUnit.DAYS), seller);
        auction.setStatus(status);
        auction.setCurrentHighestBid(currentHighest);
        ReflectionTestUtils.setField(auction, "id", 10L);
        return auction;
    }

    private Auction sealedAuction(AuctionStatus status) {
        Auction auction = new Auction("Sealed", "d", AuctionCategory.OTHER, AuctionType.SEALED_BID,
                new BigDecimal("100.00"), null,
                Instant.now().minus(1, ChronoUnit.HOURS), Instant.now().plus(1, ChronoUnit.DAYS), seller);
        auction.setStatus(status);
        ReflectionTestUtils.setField(auction, "id", 10L);
        return auction;
    }

    private Auction lockReturns(Auction auction) {
        when(auctionRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(auction));
        return auction;
    }

    private void findReturns(Auction auction) {
        when(auctionRepository.findById(anyLong())).thenReturn(Optional.of(auction));
    }

    private Page<Bid> pageOfOneBid() {
        Auction auction = englishAuction(AuctionStatus.OPEN, null);
        Bid bid = new Bid(new BigDecimal("100.00"), auction, bidder);
        ReflectionTestUtils.setField(bid, "id", 50L);
        return new PageImpl<>(List.of(bid), page, 1);
    }

    private PlaceBidRequest bid(String amount) {
        return new PlaceBidRequest(new BigDecimal(amount));
    }
}
