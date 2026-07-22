package com.bidforge.mapper;

import com.bidforge.dto.response.AuctionResponse;
import com.bidforge.dto.response.AuctionResultResponse;
import com.bidforge.dto.response.AuctionSummaryResponse;
import com.bidforge.entity.Auction;
import com.bidforge.entity.AuctionResult;

// Converts Auction entities into their response DTOs

public final class AuctionMapper {

    private AuctionMapper() {
    }

    public static AuctionSummaryResponse toSummary(Auction auction) {
        return new AuctionSummaryResponse(
                auction.getId(),
                auction.getTitle(),
                auction.getCategory(),
                auction.getAuctionType(),
                auction.getStatus(),
                auction.getStartingPrice(),
                auction.getCurrentHighestBid(),
                auction.getStartTime(),
                auction.getEndTime(),
                auction.getSeller().getUsername());
    }


    // Detailed view without a result, if auction not closed
    public static AuctionResponse toResponse(Auction auction) {
        return toResponse(auction, null);
    }


    // Detailed view including the outcome of a closed auction
    public static AuctionResponse toResponse(Auction auction, AuctionResult result) {
        AuctionResultResponse resultDto = null;
        if (result != null) {
            resultDto = new AuctionResultResponse(
                    result.getWinner() == null ? null : result.getWinner().getUsername(),
                    result.getFinalPrice(),
                    result.getClosedAt());
        }
        return new AuctionResponse(
                auction.getId(),
                auction.getTitle(),
                auction.getDescription(),
                auction.getCategory(),
                auction.getAuctionType(),
                auction.getStatus(),
                auction.getStartingPrice(),
                auction.getMinIncrement(),
                auction.getCurrentHighestBid(),
                auction.getStartTime(),
                auction.getEndTime(),
                auction.getSeller().getUsername(),
                auction.getCreatedAt(),
                auction.getUpdatedAt(),
                resultDto);
    }
}
