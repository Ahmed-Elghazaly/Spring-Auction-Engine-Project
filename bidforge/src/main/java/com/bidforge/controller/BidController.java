package com.bidforge.controller;

import com.bidforge.dto.request.PlaceBidRequest;
import com.bidforge.dto.response.BidResponse;
import com.bidforge.dto.response.MyBidResponse;
import com.bidforge.service.BidService;
import com.bidforge.util.Paging;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Set;


// Placing a bid requires a JWT
// viewing an auction's bid list is public for English auctions, sealed lists are empty/own-only until the auction closes
@RestController
@RequestMapping("/api")
public class BidController {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "amount");

    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping("/auctions/{auctionId}/bids")
    public ResponseEntity<BidResponse> placeBid(@PathVariable Long auctionId,
                                                @Valid @RequestBody PlaceBidRequest request,
                                                Authentication authentication) {
        BidResponse created = bidService.placeBid(authentication.getName(), auctionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }


    // Bid history of an auction, works both anonymously and authenticated
    // Authentication parameter is used by the service for the sealed visibility rules and is null for anonymous visitors
    @GetMapping("/auctions/{auctionId}/bids")
    public ResponseEntity<Page<BidResponse>> bidsOfAuction(
            @PathVariable Long auctionId,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        String viewer = authentication == null ? null : authentication.getName();
        return ResponseEntity.ok(
                bidService.getBidsForAuction(viewer, auctionId, Paging.of(page, size, sort, SORT_FIELDS)));
    }


    // All bids of the logged-in user
    @GetMapping("/bids/my")
    public ResponseEntity<Page<MyBidResponse>> myBids(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(
                bidService.myBids(authentication.getName(), Paging.of(page, size, sort, SORT_FIELDS)));
    }
}
