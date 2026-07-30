import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { RouterLink } from '@angular/router';
import { forkJoin, of, switchMap } from 'rxjs';
import { AuctionResponse } from '../../../core/models/auction.model';
import { AUCTION_CATEGORY_LABELS, AUCTION_TYPE_LABELS } from '../../../core/models/enums';
import { Page, emptyPage } from '../../../core/models/page.model';
import { AuctionService } from '../../../core/services/auction.service';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { categoryVisual } from '../../../shared/utils/category-visuals';

interface WonAuction {
  auction: AuctionResponse;
  visual: ReturnType<typeof categoryVisual>;
}

@Component({
  selector: 'app-my-won',
  imports: [
    RouterLink,
    CurrencyPipe,
    DatePipe,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    EmptyState,
  ],
  templateUrl: './my-won.html',
  styleUrl: './my-won.scss',
})
export class MyWon {
  private readonly auctionService = inject(AuctionService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly categoryLabels = AUCTION_CATEGORY_LABELS;
  protected readonly typeLabels = AUCTION_TYPE_LABELS;

  protected readonly wins = signal<WonAuction[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(true);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(9);

  protected readonly pageTotal = computed(() =>
    this.wins().reduce((sum, win) => sum + (win.auction.result?.finalPrice ?? 0), 0),
  );

  constructor() {
    this.load();
  }

  protected onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  private load(): void {
    this.loading.set(true);

    this.auctionService
      .wonAuctions({ page: this.pageIndex(), size: this.pageSize(), sort: 'closedAt,desc' })
      .pipe(
        switchMap((page: Page<{ id: number }>) => {
          this.total.set(page.totalElements);
          if (page.content.length === 0) return of([] as AuctionResponse[]);
          return forkJoin(page.content.map((summary) => this.auctionService.getById(summary.id)));
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (auctions) => {
          this.wins.set(
            auctions.map((auction) => ({ auction, visual: categoryVisual(auction.category) })),
          );
          this.loading.set(false);
        },
        error: () => {
          this.wins.set([]);
          this.loading.set(false);
        },
      });
  }

  protected readonly placeholder = emptyPage(9);
}
