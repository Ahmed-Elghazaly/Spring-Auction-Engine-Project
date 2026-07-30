import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { MyBidResponse } from '../../../core/models/bid.model';
import { AUCTION_TYPE_LABELS } from '../../../core/models/enums';
import { Page, emptyPage } from '../../../core/models/page.model';
import { BidService } from '../../../core/services/bid.service';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { StatusChip } from '../../../shared/components/status-chip/status-chip';

interface BidOutcome {
  label: string;
  icon: string;

  tone: 'winning' | 'losing' | 'neutral' | 'sealed';
  tooltip: string;
}

@Component({
  selector: 'app-my-bids',
  imports: [
    RouterLink,

    CurrencyPipe,
    DatePipe,
    MatTableModule,
    MatPaginatorModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    StatusChip,
    EmptyState,
  ],
  templateUrl: './my-bids.html',
  styleUrl: './my-bids.scss',
})
export class MyBids {
  private readonly bidService = inject(BidService);
  private readonly destroyRef = inject(DestroyRef);

  protected typeLabel(bid: MyBidResponse): string {
    return AUCTION_TYPE_LABELS[bid.auctionType];
  }

  protected readonly result = signal<Page<MyBidResponse>>(emptyPage(15));
  protected readonly loading = signal(true);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(15);

  protected readonly columns = ['auction', 'amount', 'placed', 'status', 'outcome'] as const;

  protected readonly leadingCount = computed(
    () => this.result().content.filter((bid) => bid.currentlyWinning === true).length,
  );

  constructor() {
    this.load();
  }

  protected describe(bid: MyBidResponse): BidOutcome {
    if (bid.auctionStatus === 'CANCELLED') {
      return {
        label: 'Cancelled',
        icon: 'block',
        tone: 'neutral',
        tooltip: 'The seller or an administrator called this auction off.',
      };
    }

    if (bid.auctionStatus === 'CLOSED') {
      return {
        label: 'Ended',
        icon: 'flag',
        tone: 'neutral',
        tooltip: 'Bidding is over — open the auction to see who won.',
      };
    }

    if (bid.auctionType === 'SEALED_BID') {
      return {
        label: 'Sealed',
        icon: 'lock',
        tone: 'sealed',
        tooltip: 'Nobody can see how this bid compares until the auction closes.',
      };
    }

    if (bid.currentlyWinning === true) {
      return {
        label: 'Leading',
        icon: 'trending_up',
        tone: 'winning',
        tooltip: 'This is currently the highest bid on the auction.',
      };
    }

    return {
      label: 'Outbid',
      icon: 'trending_down',
      tone: 'losing',
      tooltip: 'Someone has bid higher. Open the auction to bid again.',
    };
  }

  protected onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  private load(): void {
    this.loading.set(true);

    this.bidService
      .myBids({ page: this.pageIndex(), size: this.pageSize(), sort: 'createdAt,desc' })
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
