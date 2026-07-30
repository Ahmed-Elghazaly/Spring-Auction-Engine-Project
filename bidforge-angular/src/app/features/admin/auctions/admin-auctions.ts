import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { combineLatest, debounceTime, distinctUntilChanged, startWith } from 'rxjs';
import { AuctionSummaryResponse } from '../../../core/models/auction.model';
import {
  AUCTION_CATEGORIES,
  AUCTION_CATEGORY_LABELS,
  AUCTION_STATUSES,
  AUCTION_STATUS_LABELS,
  AUCTION_TYPES,
  AUCTION_TYPE_LABELS,
  AuctionCategory,
  AuctionStatus,
  AuctionType,
} from '../../../core/models/enums';
import { Page, emptyPage } from '../../../core/models/page.model';
import { AdminService } from '../../../core/services/admin.service';
import { AuctionLifecycleService } from '../../../core/services/auction-lifecycle.service';
import { AuthService } from '../../../core/services/auth.service';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { StatusChip } from '../../../shared/components/status-chip/status-chip';

@Component({
  selector: 'app-admin-auctions',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    CurrencyPipe,
    DatePipe,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    StatusChip,
    EmptyState,
  ],
  templateUrl: './admin-auctions.html',
  styleUrl: '../admin-page.scss',
})
export class AdminAuctions {
  private readonly adminService = inject(AdminService);
  private readonly lifecycle = inject(AuctionLifecycleService);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly statuses = AUCTION_STATUSES;
  protected readonly types = AUCTION_TYPES;
  protected readonly categories = AUCTION_CATEGORIES;
  protected readonly statusLabels = AUCTION_STATUS_LABELS;
  protected readonly typeLabels = AUCTION_TYPE_LABELS;
  protected readonly categoryLabels = AUCTION_CATEGORY_LABELS;

  protected readonly result = signal<Page<AuctionSummaryResponse>>(emptyPage(15));
  protected readonly loading = signal(true);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(15);
  protected readonly busyId = signal<number | null>(null);

  protected readonly columns = [
    'title',
    'seller',
    'type',
    'price',
    'ends',
    'status',
    'actions',
  ] as const;

  protected readonly status = signal<AuctionStatus | undefined>(undefined);
  protected readonly type = signal<AuctionType | undefined>(undefined);
  protected readonly category = signal<AuctionCategory | undefined>(undefined);

  protected readonly searchControl = new FormControl('', { nonNullable: true });
  protected readonly sellerControl = new FormControl('', { nonNullable: true });

  constructor() {
    combineLatest([
      this.searchControl.valueChanges.pipe(startWith('')),
      this.sellerControl.valueChanges.pipe(startWith('')),
    ])
      .pipe(debounceTime(350), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => {
        this.pageIndex.set(0);
        this.load();
      });
  }

  protected onFilterChange(): void {
    this.pageIndex.set(0);
    this.load();
  }

  protected onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  protected hasActiveFilters(): boolean {
    return (
      this.status() !== undefined ||
      this.type() !== undefined ||
      this.category() !== undefined ||
      this.searchControl.value.trim().length > 0 ||
      this.sellerControl.value.trim().length > 0
    );
  }

  protected clearFilters(): void {
    this.status.set(undefined);
    this.type.set(undefined);
    this.category.set(undefined);

    this.searchControl.setValue('', { emitEvent: false });
    this.sellerControl.setValue('', { emitEvent: false });
    this.pageIndex.set(0);
    this.load();
  }

  protected filterBySeller(username: string): void {
    this.sellerControl.setValue(username);
  }

  protected actionsFor(status: AuctionStatus) {
    return this.lifecycle.allowedActions(status, this.auth.isAdmin());
  }

  protected openAuction(auction: AuctionSummaryResponse): void {
    this.run(auction.id, this.lifecycle.open(auction.id));
  }

  protected closeAuction(auction: AuctionSummaryResponse): void {
    this.run(auction.id, this.lifecycle.close(auction.id));
  }

  protected cancelAuction(auction: AuctionSummaryResponse): void {
    this.run(auction.id, this.lifecycle.cancel(auction.id));
  }

  private run(id: number, action$: ReturnType<AuctionLifecycleService['open']>): void {
    this.busyId.set(id);
    action$.subscribe({
      next: () => this.load(),
      error: () => this.busyId.set(null),
      complete: () => this.busyId.set(null),
    });
  }

  private load(): void {
    this.loading.set(true);

    this.adminService
      .searchAuctions(
        {
          status: this.status(),
          type: this.type(),
          category: this.category(),
          q: this.searchControl.value.trim() || undefined,
          seller: this.sellerControl.value.trim() || undefined,
        },
        { page: this.pageIndex(), size: this.pageSize(), sort: 'createdAt,desc' },
      )
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
