import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { AuctionSummaryResponse } from '../../core/models/auction.model';
import { AuctionService } from '../../core/services/auction.service';
import { AuthService } from '../../core/services/auth.service';
import { AuctionCard } from '../../shared/components/auction-card/auction-card';
import { EmptyState } from '../../shared/components/empty-state/empty-state';
import { SkeletonGrid } from '../../shared/components/skeleton-grid/skeleton-grid';

@Component({
  selector: 'app-home',
  imports: [RouterLink, MatButtonModule, MatIconModule, AuctionCard, EmptyState, SkeletonGrid],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  private readonly auctionService = inject(AuctionService);
  protected readonly auth = inject(AuthService);

  protected readonly loading = signal(true);
  protected readonly auctions = signal<AuctionSummaryResponse[]>([]);

  constructor() {
    this.auctionService
      .browse({ status: 'OPEN' }, { page: 0, size: 8, sort: 'endTime,asc' })
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (page) => {
          this.auctions.set(page.content);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }
}
