import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs';
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
import { AuctionService } from '../../../core/services/auction.service';
import { AuctionCard } from '../../../shared/components/auction-card/auction-card';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { SkeletonGrid } from '../../../shared/components/skeleton-grid/skeleton-grid';

const SORT_OPTIONS = [
  { value: 'createdAt,desc', label: 'Newest first' },
  { value: 'endTime,asc', label: 'Ending soonest' },
  { value: 'startingPrice,asc', label: 'Price: low to high' },
  { value: 'startingPrice,desc', label: 'Price: high to low' },
  { value: 'currentHighestBid,desc', label: 'Highest current bid' },
];

@Component({
  selector: 'app-browse',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    AuctionCard,
    EmptyState,
    SkeletonGrid,
  ],
  templateUrl: './browse.html',
  styleUrl: './browse.scss',
})
export class Browse {
  private readonly auctionService = inject(AuctionService);

  private readonly destroyRef = inject(DestroyRef);

  protected readonly statuses = AUCTION_STATUSES;
  protected readonly types = AUCTION_TYPES;
  protected readonly categories = AUCTION_CATEGORIES;
  protected readonly statusLabels = AUCTION_STATUS_LABELS;
  protected readonly typeLabels = AUCTION_TYPE_LABELS;
  protected readonly categoryLabels = AUCTION_CATEGORY_LABELS;
  protected readonly sortOptions = SORT_OPTIONS;

  protected readonly status = signal<AuctionStatus | undefined>(undefined);
  protected readonly type = signal<AuctionType | undefined>(undefined);
  protected readonly category = signal<AuctionCategory | undefined>(undefined);
  protected readonly sort = signal<string>('createdAt,desc');
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(12);

  protected readonly result = signal<Page<AuctionSummaryResponse>>(emptyPage());
  protected readonly loading = signal(true);

  protected readonly searchControl = new FormControl('', { nonNullable: true });

  constructor() {
    this.searchControl.valueChanges
      .pipe(debounceTime(350), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => {
        this.pageIndex.set(0);
        this.load();
      });

    this.load();
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

  protected clearFilters(): void {
    this.status.set(undefined);
    this.type.set(undefined);
    this.category.set(undefined);

    this.searchControl.setValue('', { emitEvent: false });
    this.sort.set('createdAt,desc');
    this.pageIndex.set(0);
    this.load();
  }

  protected hasActiveFilters(): boolean {
    return (
      this.status() !== undefined ||
      this.type() !== undefined ||
      this.category() !== undefined ||
      this.searchControl.value.trim().length > 0
    );
  }

  private load(): void {
    this.loading.set(true);

    this.auctionService
      .browse(
        {
          status: this.status(),
          type: this.type(),
          category: this.category(),
          q: this.searchControl.value.trim() || undefined,
        },
        { page: this.pageIndex(), size: this.pageSize(), sort: this.sort() },
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
