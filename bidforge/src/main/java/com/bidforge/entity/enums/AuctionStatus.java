package com.bidforge.entity.enums;

// Clients can never set this field directly, it only changes through
// dedicated service methods, which validate every transition

public enum AuctionStatus {

    // Created and Validated, bidding has not started yet, Editable/Cancellable by the owner
    SCHEDULED,


    // Bidding is live (start time reached, or opened early by the owner/admin)
    OPEN,

    // Finished, winner determined and recorded. Terminal state
    CLOSED,


    // cancelled before completion, doesn't have a winner, Terminal state
    CANCELLED
}
