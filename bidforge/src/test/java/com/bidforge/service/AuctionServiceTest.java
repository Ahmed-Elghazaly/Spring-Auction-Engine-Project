package com.bidforge.service;

import com.bidforge.dto.request.CreateAuctionRequest;
import com.bidforge.dto.request.UpdateAuctionRequest;
import com.bidforge.dto.response.AuctionResponse;
import com.bidforge.entity.*;
import com.bidforge.entity.enums.AuctionCategory;
import com.bidforge.entity.enums.AuctionStatus;
import com.bidforge.entity.enums.AuctionType;
import com.bidforge.entity.enums.RoleName;
import com.bidforge.exception.BusinessRuleException;
import com.bidforge.exception.InvalidAuctionStateException;
import com.bidforge.exception.OwnershipException;
import com.bidforge.repository.AuctionRepository;
import com.bidforge.repository.AuctionResultRepository;
import com.bidforge.repository.BidRepository;
import com.bidforge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * PRIVATE verification tests (never submitted) for the auction lifecycle
 * rules R1-R6 and the state machine — plan §19. Pure unit tests: repositories
 * are Mockito mocks, no Spring context, no database.
 */
@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock private AuctionRepository auctionRepository;
    @Mock private UserRepository userRepository;
    @Mock private BidRepository bidRepository;
    @Mock private AuctionResultRepository auctionResultRepository;
    @Mock private AuditService auditService;

    private AuctionService service;

    private User seller;
    private User stranger;
    private User admin;
    private final Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS);
    private final Instant nextWeek = Instant.now().plus(7, ChronoUnit.DAYS);

    @BeforeEach
    void setUp() {
        service = new AuctionService(auctionRepository, userRepository, bidRepository,
                auctionResultRepository, auditService);

        seller = user(1L, "seller1");
        stranger = user(2L, "stranger");
        admin = user(3L, "admin");
        admin.addRole(new Role(RoleName.ROLE_ADMIN));

        lenient().when(userRepository.findByUsername("seller1")).thenReturn(Optional.of(seller));
        lenient().when(userRepository.findByUsername("stranger")).thenReturn(Optional.of(stranger));
        lenient().when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        lenient().when(auctionRepository.save(any(Auction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(auctionResultRepository.save(any(AuctionResult.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    /* ---------------- creation rules (R1-R6) ---------------- */

    @Test
    void create_rejectsEndTimeBeforeStartTime() { // R4
        CreateAuctionRequest request = createRequest(AuctionType.ENGLISH, new BigDecimal("1.00"),
                nextWeek, tomorrow);
        assertThatThrownBy(() -> service.create("seller1", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("endTime");
    }

    @Test
    void create_rejectsEnglishWithoutIncrement() { // R3
        CreateAuctionRequest request = createRequest(AuctionType.ENGLISH, null, tomorrow, nextWeek);
        assertThatThrownBy(() -> service.create("seller1", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("minIncrement is required");
    }

    @Test
    void create_rejectsSealedWithIncrement() { // R3
        CreateAuctionRequest request = createRequest(AuctionType.SEALED_BID, new BigDecimal("1.00"),
                tomorrow, nextWeek);
        assertThatThrownBy(() -> service.create("seller1", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("must not be set");
    }

    @Test
    void create_validEnglishAuction_startsScheduled() {
        CreateAuctionRequest request = createRequest(AuctionType.ENGLISH, new BigDecimal("5.00"),
                tomorrow, nextWeek);
        AuctionResponse response = service.create("seller1", request);

        assertThat(response.status()).isEqualTo(AuctionStatus.SCHEDULED);
        assertThat(response.sellerUsername()).isEqualTo("seller1");
        verify(auctionRepository).save(any(Auction.class));
    }

    /* ---------------- editing (R5) ---------------- */

    @Test
    void update_rejectsNonOwner() {
        lockReturns(auction(10L, AuctionType.ENGLISH, AuctionStatus.SCHEDULED));
        assertThatThrownBy(() -> service.update("stranger", 10L, updateRequest()))
                .isInstanceOf(OwnershipException.class);
    }

    @Test
    void update_rejectsWhenNotScheduled() {
        lockReturns(auction(10L, AuctionType.ENGLISH, AuctionStatus.OPEN));
        assertThatThrownBy(() -> service.update("seller1", 10L, updateRequest()))
                .isInstanceOf(InvalidAuctionStateException.class);
    }

    /* ---------------- state machine (§8) ---------------- */

    @Test
    void open_movesScheduledToOpen() {
        Auction auction = lockReturns(auction(10L, AuctionType.ENGLISH, AuctionStatus.SCHEDULED));
        service.open("seller1", 10L);
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.OPEN);
    }

    @Test
    void open_rejectsWhenAlreadyOpen() {
        lockReturns(auction(10L, AuctionType.ENGLISH, AuctionStatus.OPEN));
        assertThatThrownBy(() -> service.open("seller1", 10L))
                .isInstanceOf(InvalidAuctionStateException.class);
    }

    @Test
    void open_rejectsStrangers() {
        lockReturns(auction(10L, AuctionType.ENGLISH, AuctionStatus.SCHEDULED));
        assertThatThrownBy(() -> service.open("stranger", 10L))
                .isInstanceOf(OwnershipException.class);
    }

    @Test
    void cancel_ownerCanCancelScheduled() {
        Auction auction = lockReturns(auction(10L, AuctionType.ENGLISH, AuctionStatus.SCHEDULED));
        service.cancel("seller1", 10L);
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CANCELLED);
    }

    @Test
    void cancel_ownerCannotCancelOpen_adminCan() {
        Auction auction = lockReturns(auction(10L, AuctionType.ENGLISH, AuctionStatus.OPEN));
        assertThatThrownBy(() -> service.cancel("seller1", 10L))
                .isInstanceOf(InvalidAuctionStateException.class);

        service.cancel("admin", 10L);
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CANCELLED);
    }

    @Test
    void cancel_rejectsTerminalStates() {
        lockReturns(auction(10L, AuctionType.ENGLISH, AuctionStatus.CLOSED));
        assertThatThrownBy(() -> service.cancel("admin", 10L))
                .isInstanceOf(InvalidAuctionStateException.class);
    }

    /* ---------------- closing & winner (R19-R23) ---------------- */

    @Test
    void close_picksRepositoryTopBidAsWinner() { // R20 — ordering delegated to the ranked query
        Auction auction = lockReturns(auction(10L, AuctionType.ENGLISH, AuctionStatus.OPEN));
        Bid top = new Bid(new BigDecimal("450.00"), auction, stranger);
        when(bidRepository.findFirstByAuctionIdOrderByAmountDescCreatedAtAsc(10L))
                .thenReturn(Optional.of(top));

        AuctionResponse response = service.close("seller1", 10L);

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CLOSED);
        assertThat(response.result().winnerUsername()).isEqualTo("stranger");
        assertThat(response.result().finalPrice()).isEqualByComparingTo("450.00");
    }

    @Test
    void close_withNoBids_hasNoWinner() { // R21
        Auction auction = lockReturns(auction(10L, AuctionType.ENGLISH, AuctionStatus.OPEN));
        when(bidRepository.findFirstByAuctionIdOrderByAmountDescCreatedAtAsc(10L))
                .thenReturn(Optional.empty());

        AuctionResponse response = service.close("seller1", 10L);

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CLOSED);
        assertThat(response.result().winnerUsername()).isNull();
        assertThat(response.result().finalPrice()).isNull();
    }

    @Test
    void close_rejectsWhenNotOpen() { // R19 — closing twice is impossible
        lockReturns(auction(10L, AuctionType.ENGLISH, AuctionStatus.CLOSED));
        assertThatThrownBy(() -> service.close("seller1", 10L))
                .isInstanceOf(InvalidAuctionStateException.class);
        verify(auctionResultRepository, never()).save(any());
    }

    /* ---------------- scheduler variants ---------------- */

    @Test
    void openBySystem_silentlySkipsWhenStatusChanged() {
        Auction auction = lockReturns(auction(10L, AuctionType.ENGLISH, AuctionStatus.CANCELLED));
        service.openBySystem(10L); // must not throw
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CANCELLED);
    }

    @Test
    void closeBySystem_closesOpenAuction() {
        Auction auction = lockReturns(auction(10L, AuctionType.ENGLISH, AuctionStatus.OPEN));
        when(bidRepository.findFirstByAuctionIdOrderByAmountDescCreatedAtAsc(10L))
                .thenReturn(Optional.empty());
        service.closeBySystem(10L);
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CLOSED);
    }

    /* ---------------- helpers ---------------- */

    private User user(Long id, String username) {
        User user = new User(username, username + "@x.com", "hash", "F", "L");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Auction auction(Long id, AuctionType type, AuctionStatus status) {
        Auction auction = new Auction("Test auction", "d", AuctionCategory.OTHER, type,
                new BigDecimal("100.00"),
                type == AuctionType.ENGLISH ? new BigDecimal("10.00") : null,
                tomorrow, nextWeek, seller);
        auction.setStatus(status);
        ReflectionTestUtils.setField(auction, "id", id);
        return auction;
    }

    private Auction lockReturns(Auction auction) {
        when(auctionRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(auction));
        return auction;
    }

    private CreateAuctionRequest createRequest(AuctionType type, BigDecimal increment,
                                               Instant start, Instant end) {
        return new CreateAuctionRequest("Test auction", "d", AuctionCategory.OTHER, type,
                new BigDecimal("100.00"), increment, start, end);
    }

    private UpdateAuctionRequest updateRequest() {
        return new UpdateAuctionRequest("New title", "d", AuctionCategory.OTHER,
                new BigDecimal("100.00"), new BigDecimal("10.00"), tomorrow, nextWeek);
    }
}
