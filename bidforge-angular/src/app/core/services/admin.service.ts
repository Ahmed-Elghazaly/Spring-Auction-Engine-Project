import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { toHttpParams } from '../http/http-params.util';
import { AuctionFilters, AuctionSummaryResponse } from '../models/auction.model';
import { AuditEventResponse, AuditFilters } from '../models/audit.model';
import { Page, PageQuery } from '../models/page.model';
import { UpdateUserStatusRequest, UserResponse } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/admin`;

  listUsers(page: PageQuery): Observable<Page<UserResponse>> {
    const params = toHttpParams({ page: page.page, size: page.size, sort: page.sort });
    return this.http.get<Page<UserResponse>>(`${this.baseUrl}/users`, { params });
  }

  updateUserStatus(userId: number, enabled: boolean): Observable<UserResponse> {
    const body: UpdateUserStatusRequest = { enabled };
    return this.http.patch<UserResponse>(`${this.baseUrl}/users/${userId}/status`, body);
  }

  searchAuctions(
    filters: AuctionFilters,
    page: PageQuery,
  ): Observable<Page<AuctionSummaryResponse>> {
    const params = toHttpParams({
      status: filters.status,
      type: filters.type,
      category: filters.category,
      q: filters.q,
      seller: filters.seller,
      page: page.page,
      size: page.size,
      sort: page.sort,
    });
    return this.http.get<Page<AuctionSummaryResponse>>(`${this.baseUrl}/auctions`, { params });
  }

  auditEvents(filters: AuditFilters, page: PageQuery): Observable<Page<AuditEventResponse>> {
    const params = toHttpParams({
      entityType: filters.entityType,
      entityId: filters.entityId,
      actor: filters.actor,
      page: page.page,
      size: page.size,
      sort: page.sort,
    });
    return this.http.get<Page<AuditEventResponse>>(`${this.baseUrl}/audit-events`, { params });
  }
}
