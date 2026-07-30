import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { toHttpParams } from '../http/http-params.util';
import {
  AuctionFilters,
  AuctionResponse,
  AuctionSummaryResponse,
  CreateAuctionRequest,
  UpdateAuctionRequest,
} from '../models/auction.model';
import { Page, PageQuery } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class AuctionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/auctions`;

  browse(filters: AuctionFilters, page: PageQuery): Observable<Page<AuctionSummaryResponse>> {
    const params = toHttpParams({
      status: filters.status,
      type: filters.type,
      category: filters.category,
      q: filters.q,
      page: page.page,
      size: page.size,
      sort: page.sort,
    });
    return this.http.get<Page<AuctionSummaryResponse>>(this.baseUrl, { params });
  }

  getById(id: number): Observable<AuctionResponse> {
    return this.http.get<AuctionResponse>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateAuctionRequest): Observable<AuctionResponse> {
    return this.http.post<AuctionResponse>(this.baseUrl, request);
  }

  update(id: number, request: UpdateAuctionRequest): Observable<AuctionResponse> {
    return this.http.put<AuctionResponse>(`${this.baseUrl}/${id}`, request);
  }

  open(id: number): Observable<AuctionResponse> {
    return this.http.post<AuctionResponse>(`${this.baseUrl}/${id}/open`, {});
  }

  close(id: number): Observable<AuctionResponse> {
    return this.http.post<AuctionResponse>(`${this.baseUrl}/${id}/close`, {});
  }

  cancel(id: number): Observable<AuctionResponse> {
    return this.http.post<AuctionResponse>(`${this.baseUrl}/${id}/cancel`, {});
  }

  myAuctions(page: PageQuery): Observable<Page<AuctionSummaryResponse>> {
    const params = toHttpParams({ page: page.page, size: page.size, sort: page.sort });
    return this.http.get<Page<AuctionSummaryResponse>>(`${this.baseUrl}/mine`, { params });
  }

  wonAuctions(page: PageQuery): Observable<Page<AuctionSummaryResponse>> {
    const params = toHttpParams({ page: page.page, size: page.size, sort: page.sort });
    return this.http.get<Page<AuctionSummaryResponse>>(`${this.baseUrl}/won`, { params });
  }
}
