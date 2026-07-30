import { AuctionStatus, AuctionType } from './enums';

export interface BidResponse {
  id: number;
  auctionId: number;
  bidderUsername: string;
  amount: number;

  createdAt: string;
}

export interface MyBidResponse {
  id: number;
  auctionId: number;
  auctionTitle: string;
  auctionType: AuctionType;
  auctionStatus: AuctionStatus;
  amount: number;
  createdAt: string;

  currentlyWinning: boolean | null;
}

export interface PlaceBidRequest {
  amount: number;
}
