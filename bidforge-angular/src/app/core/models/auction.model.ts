import { AuctionCategory, AuctionStatus, AuctionType } from './enums';

export interface AuctionSummaryResponse {
  id: number;
  title: string;
  category: AuctionCategory;
  auctionType: AuctionType;
  status: AuctionStatus;
  startingPrice: number;

  currentHighestBid?: number | null;

  startTime: string;

  endTime: string;
  sellerUsername: string;
}

export interface AuctionResultResponse {
  winnerUsername?: string | null;
  finalPrice?: number | null;
  closedAt?: string | null;
}

export interface AuctionResponse {
  id: number;
  title: string;
  description?: string | null;
  category: AuctionCategory;
  auctionType: AuctionType;
  status: AuctionStatus;
  startingPrice: number;

  minIncrement?: number | null;
  currentHighestBid?: number | null;
  startTime: string;
  endTime: string;
  sellerUsername: string;
  createdAt: string;
  updatedAt: string;

  result?: AuctionResultResponse | null;
}

export interface CreateAuctionRequest {
  title: string;
  description: string | null;
  category: AuctionCategory;
  auctionType: AuctionType;
  startingPrice: number;

  minIncrement: number | null;
  startTime: string;
  endTime: string;
}

export interface UpdateAuctionRequest {
  title: string;
  description: string | null;
  category: AuctionCategory;
  startingPrice: number;
  minIncrement: number | null;
  startTime: string;
  endTime: string;
}

export interface AuctionFilters {
  status?: AuctionStatus;
  type?: AuctionType;
  category?: AuctionCategory;

  q?: string;

  seller?: string;
}
