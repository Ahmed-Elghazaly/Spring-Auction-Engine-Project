import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, input, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroupDirective, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { Observable, forkJoin, interval } from 'rxjs';
import { AuctionResponse } from '../../../core/models/auction.model';
import { BidResponse } from '../../../core/models/bid.model';
import {
  AUCTION_CATEGORY_LABELS,
  AUCTION_TYPE_HINTS,
  AUCTION_TYPE_LABELS,
} from '../../../core/models/enums';
import { Page, emptyPage } from '../../../core/models/page.model';
import { AuctionLifecycleService } from '../../../core/services/auction-lifecycle.service';
import { AuctionService } from '../../../core/services/auction.service';
import { AuthService } from '../../../core/services/auth.service';
import { BidService } from '../../../core/services/bid.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Countdown } from '../../../shared/components/countdown/countdown';
import { StatusChip } from '../../../shared/components/status-chip/status-chip';
import { categoryVisual } from '../../../shared/utils/category-visuals';

const POLL_INTERVAL_MS = 5000;

@Component({
  selector: 'app-auction-detail',
  imports: [
    RouterLink,
    ReactiveFormsModule,

    CurrencyPipe,
    DatePipe,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatDividerModule,
    MatProgressBarModule,
    MatTooltipModule,
    StatusChip,
    Countdown,
  ],
  templateUrl: './detail.html',
  styleUrl: './detail.scss',
})
export class AuctionDetail {
  private readonly auctionService = inject(AuctionService);
  private readonly bidService = inject(BidService);
  private readonly notifications = inject(NotificationService);
  private readonly lifecycle = inject(AuctionLifecycleService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly auth = inject(AuthService);

  readonly id = input.required<string>();

  protected readonly auction = signal<AuctionResponse | null>(null);
  protected readonly bids = signal<Page<BidResponse>>(emptyPage(50));
  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);

  protected readonly busy = signal(false);

  protected readonly typeLabels = AUCTION_TYPE_LABELS;
  protected readonly typeHints = AUCTION_TYPE_HINTS;

  private readonly formBuilder = inject(FormBuilder);

  protected readonly bidForm = this.formBuilder.group({
    amount: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
    ]),
  });

  private readonly bidFormDirective = viewChild(FormGroupDirective);

  constructor() {
    toObservable(this.id)
      .pipe(takeUntilDestroyed())
      .subscribe((id) => this.load(Number(id), true));

    interval(POLL_INTERVAL_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        const current = this.auction();
        if (!current || document.visibilityState !== 'visible') return;
        if (current.status === 'OPEN' || current.status === 'SCHEDULED') {
          this.load(current.id, false);
        }
      });
  }

  protected readonly visual = computed(() => {
    const current = this.auction();
    return current ? categoryVisual(current.category) : null;
  });

  protected readonly categoryLabel = computed(() => {
    const current = this.auction();
    return current ? AUCTION_CATEGORY_LABELS[current.category] : '';
  });

  protected readonly isSealed = computed(() => this.auction()?.auctionType === 'SEALED_BID');
  protected readonly isOpen = computed(() => this.auction()?.status === 'OPEN');
  protected readonly isClosed = computed(() => this.auction()?.status === 'CLOSED');

  protected readonly isSeller = computed(
    () => !!this.auth.username() && this.auction()?.sellerUsername === this.auth.username(),
  );

  protected readonly canManage = computed(() => this.isSeller() || this.auth.isAdmin());

  protected readonly actions = computed(() => {
    const current = this.auction();
    if (!current) return null;
    return this.lifecycle.allowedActions(current.status, this.auth.isAdmin());
  });

  protected readonly mySealedBid = computed(() => {
    const current = this.auction();
    const me = this.auth.username();
    if (!current || current.auctionType !== 'SEALED_BID' || !me) return null;
    return this.bids().content.find((bid) => bid.bidderUsername === me) ?? null;
  });

  protected readonly minimumBid = computed<number | null>(() => {
    const current = this.auction();
    if (!current) return null;

    if (current.auctionType === 'SEALED_BID') return current.startingPrice;

    const highest = current.currentHighestBid ?? null;
    if (highest === null) return current.startingPrice;
    return highest + (current.minIncrement ?? 0);
  });

  protected readonly minimumBidLabel = computed<string | null>(() => {
    if (!this.isOpen() || this.minimumBid() === null) return null;
    if (this.mySealedBid()) return null;
    return this.isSealed() ? 'Minimum bid:' : 'Next valid bid:';
  });

  protected readonly hasHighestBid = computed(
    () => (this.auction()?.currentHighestBid ?? null) !== null,
  );

  protected readonly displayPrice = computed(() => {
    const current = this.auction();
    if (!current) return 0;
    return current.currentHighestBid ?? current.startingPrice;
  });

  protected readonly bidBlockedReason = computed<string | null>(() => {
    const current = this.auction();
    if (!current) return null;
    if (!this.auth.isLoggedIn()) return 'Sign in to place a bid on this auction.';
    if (this.isSeller()) return 'This is your own auction. Sellers cannot bid on what they list.';

    switch (current.status) {
      case 'SCHEDULED':
        return 'Bidding has not started yet. This auction opens at its start time.';
      case 'CLOSED':
        return 'This auction has closed.';
      case 'CANCELLED':
        return 'This auction was cancelled.';
    }

    if (this.mySealedBid()) {
      return 'You have already placed your sealed bid — sealed bids are final.';
    }
    return null;
  });

  protected readonly canBid = computed(() => this.bidBlockedReason() === null);

  protected readonly bidListNote = computed<string | null>(() => {
    if (!this.isSealed()) return null;
    if (this.isClosed()) return 'This sealed auction has closed, so every bid is now revealed.';
    if (!this.auth.isLoggedIn()) {
      return 'Bids in a sealed auction stay secret until it closes. Sign in to see your own bid.';
    }
    return 'Bids stay secret until this auction closes — you can only see your own.';
  });

  protected placeBid(): void {
    const current = this.auction();
    const amount = this.bidForm.controls.amount.value;
    if (!current || amount === null || this.bidForm.invalid) {
      this.bidForm.markAllAsTouched();
      return;
    }

    this.busy.set(true);
    this.bidService.placeBid(current.id, { amount }).subscribe({
      next: (bid) => {
        this.busy.set(false);

        this.bidFormDirective()?.resetForm();
        this.notifications.success(
          this.isSealed()
            ? `Sealed bid of $${bid.amount} placed — it stays hidden until the auction closes.`
            : `Bid of $${bid.amount} placed. You are the highest bidder.`,
        );
        this.load(current.id, false);
      },

      error: () => this.busy.set(false),
    });
  }

  protected useMinimumBid(): void {
    const minimum = this.minimumBid();
    if (minimum !== null) this.bidForm.controls.amount.setValue(minimum);
  }

  protected openAuction(): void {
    this.runLifecycle((id) => this.lifecycle.open(id));
  }

  protected closeAuction(): void {
    this.runLifecycle((id) => this.lifecycle.close(id));
  }

  protected cancelAuction(): void {
    this.runLifecycle((id) => this.lifecycle.cancel(id));
  }

  private runLifecycle(action: (id: number) => Observable<AuctionResponse>): void {
    const current = this.auction();
    if (!current) return;

    this.busy.set(true);
    action(current.id).subscribe({
      next: (updated) => {
        this.auction.set(updated);
        this.load(updated.id, false);
      },
      error: () => this.busy.set(false),
      complete: () => this.busy.set(false),
    });
  }

  private load(id: number, showSpinner: boolean): void {
    if (Number.isNaN(id)) {
      this.notFound.set(true);
      this.loading.set(false);
      return;
    }

    if (showSpinner) this.loading.set(true);

    forkJoin({
      auction: this.auctionService.getById(id),
      bids: this.bidService.getBidsForAuction(id, { page: 0, size: 50, sort: 'createdAt,desc' }),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ auction, bids }) => {
          this.auction.set(auction);
          this.bids.set(bids);
          this.loading.set(false);
          this.notFound.set(false);
        },
        error: () => {
          this.loading.set(false);
          if (showSpinner) this.notFound.set(true);
        },
      });
  }
}
