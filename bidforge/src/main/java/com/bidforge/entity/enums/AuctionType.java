package com.bidforge.entity.enums;


public enum AuctionType {


    // everyone sees the current highest bid
    // every new bid must beat it by at least the auction's minimum increment
    // highest bid at closing time wins
    ENGLISH,

    // bids are secret, each participant submits only one final bid (at least the starting price) without seeing anyone else's bids.
    // when the auction closes, all bids are revealed and the highest wins
    // ties go to the earliest bid.
    SEALED_BID
}
