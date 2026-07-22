package com.bidforge.mapper;

import com.bidforge.dto.response.BidResponse;
import com.bidforge.dto.response.MyBidResponse;
import com.bidforge.entity.Auction;
import com.bidforge.entity.Bid;
import com.bidforge.entity.enums.AuctionStatus;
import com.bidforge.entity.enums.AuctionType;

// Converts Bid entities into their response DTOs

public final class BidMapper {

    private BidMapper() {
    }

    public static BidResponse toResponse(Bid bid) {
        return new BidResponse(
                bid.getId(),
                bid.getAuction().getId(),
                bid.getBidder().getUsername(),
                bid.getAmount(),
                bid.getCreatedAt());
    }

    public static MyBidResponse toMyBidResponse(Bid bid) {
        Auction auction = bid.getAuction();

        // computed and not stored
        // Only defined while an English auction is OPEN
        Boolean currentlyWinning = null;
        if (auction.getAuctionType() == AuctionType.ENGLISH
                && auction.getStatus() == AuctionStatus.OPEN
                && auction.getCurrentHighestBid() != null) {
            currentlyWinning = bid.getAmount().compareTo(auction.getCurrentHighestBid()) == 0;
        }

        return new MyBidResponse(
                bid.getId(),
                auction.getId(),
                auction.getTitle(),
                auction.getAuctionType(),
                auction.getStatus(),
                bid.getAmount(),
                bid.getCreatedAt(),
                currentlyWinning);
    }
}
