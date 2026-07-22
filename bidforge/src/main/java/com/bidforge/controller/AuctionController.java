package com.bidforge.controller;

import com.bidforge.dto.request.CreateAuctionRequest;
import com.bidforge.dto.request.UpdateAuctionRequest;
import com.bidforge.dto.response.AuctionResponse;
import com.bidforge.dto.response.AuctionSummaryResponse;
import com.bidforge.entity.enums.AuctionCategory;
import com.bidforge.entity.enums.AuctionStatus;
import com.bidforge.entity.enums.AuctionType;
import com.bidforge.service.AuctionService;
import com.bidforge.util.Paging;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Set;


// Auction endpoints
// every write requires a JWT

@RestController
@RequestMapping("/api/auctions")
public class AuctionController {


    // Sort fields the auction list endpoints allow
    private static final Set<String> SORT_FIELDS =
            Set.of("createdAt", "startTime", "endTime", "startingPrice", "currentHighestBid");

    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }


    // Create an auction, for any authenticated user
    // returns 201 Created
    @PostMapping
    public ResponseEntity<AuctionResponse> create(@Valid @RequestBody CreateAuctionRequest request,
                                                  Authentication authentication) {
        AuctionResponse created = auctionService.create(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }


    // Public browse with optional filters
    // example: GET /api/auctions?status=OPEN&type=ENGLISH&category=ART&q=vase&page=0&size=10&sort=endTime,asc
    @GetMapping
    public ResponseEntity<Page<AuctionSummaryResponse>> browse(
            @RequestParam(required = false) AuctionStatus status,
            @RequestParam(required = false) AuctionType type,
            @RequestParam(required = false) AuctionCategory category,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(
                auctionService.search(status, type, category, q, Paging.of(page, size, sort, SORT_FIELDS)));
    }


    // Auctions created by the logged-in user
    @GetMapping("/mine")
    public ResponseEntity<Page<AuctionSummaryResponse>> mine(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(
                auctionService.myAuctions(authentication.getName(), Paging.of(page, size, sort, SORT_FIELDS)));
    }


    // Auctions the calling user won, sorted by closing time by default
    // results have their own sort fields, unlike the other lists
    @GetMapping("/won")
    public ResponseEntity<Page<AuctionSummaryResponse>> won(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(auctionService.wonAuctions(
                authentication.getName(),
                Paging.of(page, size, sort, Set.of("closedAt", "finalPrice"), "closedAt")));
    }


    // Public auction detail, includes the result once closed
    @GetMapping("/{id}")
    public ResponseEntity<AuctionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.getById(id));
    }


    // edit auction while SCHEDULED, for owner only
    @PutMapping("/{id}")
    public ResponseEntity<AuctionResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateAuctionRequest request,
                                                  Authentication authentication) {
        return ResponseEntity.ok(auctionService.update(authentication.getName(), id, request));
    }


    // Open auction early, for owner or admin
    @PostMapping("/{id}/open")
    public ResponseEntity<AuctionResponse> open(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(auctionService.open(authentication.getName(), id));
    }


    // Close auction and determine winner, for owner or admin
    @PostMapping("/{id}/close")
    public ResponseEntity<AuctionResponse> close(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(auctionService.close(authentication.getName(), id));
    }


    // cancel auction, for owner while auction is SCHEDULED and for admin SCHEDULED/OPEN
    @PostMapping("/{id}/cancel")
    public ResponseEntity<AuctionResponse> cancel(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(auctionService.cancel(authentication.getName(), id));
    }
}
