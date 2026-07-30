import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { toHttpParams } from '../http/http-params.util';
import { BidResponse, MyBidResponse, PlaceBidRequest } from '../models/bid.model';
import { Page, PageQuery } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class BidService {
  private readonly http = inject(HttpClient);

  placeBid(auctionId: number, request: PlaceBidRequest): Observable<BidResponse> {
    return this.http.post<BidResponse>(
      `${environment.apiBaseUrl}/auctions/${auctionId}/bids`,
      request,
    );
  }

  getBidsForAuction(auctionId: number, page: PageQuery): Observable<Page<BidResponse>> {
    const params = toHttpParams({ page: page.page, size: page.size, sort: page.sort });
    return this.http.get<Page<BidResponse>>(
      `${environment.apiBaseUrl}/auctions/${auctionId}/bids`,
      { params },
    );
  }

  myBids(page: PageQuery): Observable<Page<MyBidResponse>> {
    const params = toHttpParams({ page: page.page, size: page.size, sort: page.sort });
    return this.http.get<Page<MyBidResponse>>(`${environment.apiBaseUrl}/bids/my`, { params });
  }
}
