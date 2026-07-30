import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { AuctionSummaryResponse } from '../../../core/models/auction.model';
import { AUCTION_STATUS_LABELS, AuctionStatus } from '../../../core/models/enums';
import { Page, emptyPage } from '../../../core/models/page.model';
import { AuctionLifecycleService } from '../../../core/services/auction-lifecycle.service';
import { AuctionService } from '../../../core/services/auction.service';
import { AuthService } from '../../../core/services/auth.service';
import { AuctionCard } from '../../../shared/components/auction-card/auction-card';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { SkeletonGrid } from '../../../shared/components/skeleton-grid/skeleton-grid';

@Component({
  selector: 'app-my-auctions',
  imports: [
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    MatTooltipModule,
    AuctionCard,
    EmptyState,
    SkeletonGrid,
  ],
  templateUrl: './my-auctions.html',
  styleUrl: '../my-page.scss',
})
export class MyAuctions {
  private readonly auctionService = inject(AuctionService);
  private readonly lifecycle = inject(AuctionLifecycleService);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly statusLabels = AUCTION_STATUS_LABELS;

  protected readonly result = signal<Page<AuctionSummaryResponse>>(emptyPage(12));
  protected readonly loading = signal(true);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(12);

  protected readonly tallies = computed(() => {
    const counts: Record<AuctionStatus, number> = {
      SCHEDULED: 0,
      OPEN: 0,
      CLOSED: 0,
      CANCELLED: 0,
    };
    for (const auction of this.result().content) counts[auction.status]++;

    return (['OPEN', 'SCHEDULED', 'CLOSED', 'CANCELLED'] as const)
      .map((status) => ({ status, count: counts[status] }))
      .filter((entry) => entry.count > 0);
  });

  protected readonly busyId = signal<number | null>(null);

  constructor() {
    this.load();
  }

  protected actionsFor(status: AuctionStatus) {
    return this.lifecycle.allowedActions(status, this.auth.isAdmin());
  }

  protected openAuction(id: number): void {
    this.run(id, this.lifecycle.open(id));
  }

  protected closeAuction(id: number): void {
    this.run(id, this.lifecycle.close(id));
  }

  protected cancelAuction(id: number): void {
    this.run(id, this.lifecycle.cancel(id));
  }

  private run(id: number, action$: Observable<AuctionSummaryResponse | unknown>): void {
    this.busyId.set(id);
    action$.subscribe({
      next: () => this.load(),
      error: () => this.busyId.set(null),
      complete: () => this.busyId.set(null),
    });
  }

  protected onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  private load(): void {
    this.loading.set(true);

    this.auctionService
      .myAuctions({ page: this.pageIndex(), size: this.pageSize(), sort: 'createdAt,desc' })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.result.set(page);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }
}
