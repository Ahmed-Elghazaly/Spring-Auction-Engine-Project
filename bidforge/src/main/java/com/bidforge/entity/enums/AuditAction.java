package com.bidforge.entity.enums;

// Everything that the log can record
public enum AuditAction {
    USER_REGISTERED,
    USER_STATUS_CHANGED,
    AUCTION_CREATED,
    AUCTION_UPDATED,
    AUCTION_OPENED,
    AUCTION_CLOSED,
    AUCTION_CANCELLED,
    BID_PLACED,
    WINNER_SELECTED
}
