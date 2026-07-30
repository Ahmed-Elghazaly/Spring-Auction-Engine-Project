export type AuctionStatus = 'SCHEDULED' | 'OPEN' | 'CLOSED' | 'CANCELLED';

export const AUCTION_STATUSES: readonly AuctionStatus[] = [
  'SCHEDULED',
  'OPEN',
  'CLOSED',
  'CANCELLED',
];

export const AUCTION_STATUS_LABELS: Record<AuctionStatus, string> = {
  SCHEDULED: 'Scheduled',
  OPEN: 'Open',
  CLOSED: 'Closed',
  CANCELLED: 'Cancelled',
};

export type AuctionType = 'ENGLISH' | 'SEALED_BID';

export const AUCTION_TYPES: readonly AuctionType[] = ['ENGLISH', 'SEALED_BID'];

export const AUCTION_TYPE_LABELS: Record<AuctionType, string> = {
  ENGLISH: 'English',
  SEALED_BID: 'Sealed bid',
};

export const AUCTION_TYPE_HINTS: Record<AuctionType, string> = {
  ENGLISH:
    'Open bidding — everyone sees the current highest bid, and each new bid must beat it by the minimum increment.',
  SEALED_BID:
    'Secret bidding — each participant places one final bid that nobody else can see until the auction closes.',
};

export type AuctionCategory =
  'ELECTRONICS' | 'VEHICLES' | 'ART' | 'COLLECTIBLES' | 'FASHION' | 'BOOKS' | 'SPORTS' | 'OTHER';

export const AUCTION_CATEGORIES: readonly AuctionCategory[] = [
  'ELECTRONICS',
  'VEHICLES',
  'ART',
  'COLLECTIBLES',
  'FASHION',
  'BOOKS',
  'SPORTS',
  'OTHER',
];

export const AUCTION_CATEGORY_LABELS: Record<AuctionCategory, string> = {
  ELECTRONICS: 'Electronics',
  VEHICLES: 'Vehicles',
  ART: 'Art',
  COLLECTIBLES: 'Collectibles',
  FASHION: 'Fashion',
  BOOKS: 'Books',
  SPORTS: 'Sports',
  OTHER: 'Other',
};

export type AuditAction =
  | 'USER_REGISTERED'
  | 'USER_STATUS_CHANGED'
  | 'AUCTION_CREATED'
  | 'AUCTION_UPDATED'
  | 'AUCTION_OPENED'
  | 'AUCTION_CLOSED'
  | 'AUCTION_CANCELLED'
  | 'BID_PLACED'
  | 'WINNER_SELECTED';

export const AUDIT_ACTION_LABELS: Record<AuditAction, string> = {
  USER_REGISTERED: 'User registered',
  USER_STATUS_CHANGED: 'User status changed',
  AUCTION_CREATED: 'Auction created',
  AUCTION_UPDATED: 'Auction updated',
  AUCTION_OPENED: 'Auction opened',
  AUCTION_CLOSED: 'Auction closed',
  AUCTION_CANCELLED: 'Auction cancelled',
  BID_PLACED: 'Bid placed',
  WINNER_SELECTED: 'Winner selected',
};

export const ROLE_USER = 'ROLE_USER';
export const ROLE_ADMIN = 'ROLE_ADMIN';
