import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { AuctionResponse } from '../../../core/models/auction.model';
import {
  AUCTION_CATEGORIES,
  AUCTION_CATEGORY_LABELS,
  AUCTION_TYPES,
  AUCTION_TYPE_HINTS,
  AUCTION_TYPE_LABELS,
  AuctionCategory,
  AuctionType,
} from '../../../core/models/enums';
import { AuctionService } from '../../../core/services/auction.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  isoToLocalInput,
  localInputFromNow,
  localInputToIso,
} from '../../../shared/utils/datetime.util';
import {
  endAfterStartValidator,
  futureDateTimeValidator,
  twoDecimalsValidator,
} from '../../../shared/utils/validators';

const DEFAULT_START_OFFSET_MINUTES = 60;
const DEFAULT_END_OFFSET_MINUTES = 60 * 24 * 7;

@Component({
  selector: 'app-auction-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatProgressBarModule,
    MatTooltipModule,
  ],
  templateUrl: './auction-form.html',
  styleUrl: './auction-form.scss',
})
export class AuctionForm {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auctionService = inject(AuctionService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly id = input<string | undefined>(undefined);

  protected readonly isEdit = computed(() => this.id() !== undefined);

  protected readonly existing = signal<AuctionResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly submitting = signal(false);

  protected readonly blockedReason = signal<string | null>(null);

  protected readonly categories = AUCTION_CATEGORIES;
  protected readonly categoryLabels = AUCTION_CATEGORY_LABELS;
  protected readonly types = AUCTION_TYPES;
  protected readonly typeLabels = AUCTION_TYPE_LABELS;
  protected readonly typeHints = AUCTION_TYPE_HINTS;

  protected readonly minDateTime = localInputFromNow(0);

  protected readonly form = this.formBuilder.group(
    {
      title: this.formBuilder.nonNullable.control('', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(100),
      ]),

      description: this.formBuilder.nonNullable.control('', [Validators.maxLength(2000)]),

      category: this.formBuilder.control<AuctionCategory | null>(null, [Validators.required]),

      auctionType: this.formBuilder.nonNullable.control<AuctionType>('ENGLISH', [
        Validators.required,
      ]),

      startingPrice: this.formBuilder.control<number | null>(null, [
        Validators.required,
        Validators.min(0.01),
        twoDecimalsValidator,
      ]),

      minIncrement: this.formBuilder.control<number | null>(null, [
        Validators.min(0.01),
        twoDecimalsValidator,
      ]),

      startTime: this.formBuilder.nonNullable.control(
        localInputFromNow(DEFAULT_START_OFFSET_MINUTES),
        [Validators.required, futureDateTimeValidator],
      ),

      endTime: this.formBuilder.nonNullable.control(localInputFromNow(DEFAULT_END_OFFSET_MINUTES), [
        Validators.required,
        futureDateTimeValidator,
      ]),
    },
    {
      validators: [endAfterStartValidator('startTime', 'endTime')],
    },
  );

  protected readonly isSealed = signal(false);

  constructor() {
    this.form.controls.auctionType.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((type) => this.applyIncrementRule(type));

    this.applyIncrementRule(this.form.controls.auctionType.value);

    effect(() => {
      const routeId = this.id();
      if (routeId === undefined) return;
      this.load(Number(routeId));
    });
  }

  protected readonly heading = computed(() =>
    this.isEdit() ? 'Edit auction' : 'Create an auction',
  );

  protected readonly submitLabel = computed(() =>
    this.isEdit() ? 'Save changes' : 'Create auction',
  );

  protected endBeforeStart(): boolean {
    return this.form.hasError('endBeforeStart');
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const startTime = localInputToIso(value.startTime);
    const endTime = localInputToIso(value.endTime);
    if (!startTime || !endTime) return;

    const description = value.description.trim() || null;

    const minIncrement = value.auctionType === 'SEALED_BID' ? null : value.minIncrement;

    this.submitting.set(true);

    const editing = this.existing();
    const request$ = editing
      ? this.auctionService.update(editing.id, {
          title: value.title.trim(),
          description,
          category: value.category!,
          startingPrice: value.startingPrice!,
          minIncrement,
          startTime,
          endTime,
        })
      : this.auctionService.create({
          title: value.title.trim(),
          description,
          category: value.category!,
          auctionType: value.auctionType,
          startingPrice: value.startingPrice!,
          minIncrement,
          startTime,
          endTime,
        });

    request$.subscribe({
      next: (auction) => {
        this.submitting.set(false);
        this.notifications.success(
          editing
            ? 'Auction updated.'
            : 'Auction created — it will open automatically at its start time.',
        );
        void this.router.navigate(['/auctions', auction.id]);
      },

      error: () => this.submitting.set(false),
    });
  }

  private applyIncrementRule(type: AuctionType): void {
    const control = this.form.controls.minIncrement;
    this.isSealed.set(type === 'SEALED_BID');

    if (type === 'SEALED_BID') {
      control.clearValidators();

      control.setValue(null);
    } else {
      control.setValidators([Validators.required, Validators.min(0.01), twoDecimalsValidator]);
    }

    control.updateValueAndValidity();
  }

  private load(id: number): void {
    this.existing.set(null);
    this.blockedReason.set(null);

    if (Number.isNaN(id)) {
      this.blockedReason.set('That auction does not exist.');
      return;
    }

    this.loading.set(true);
    this.auctionService.getById(id).subscribe({
      next: (auction) => {
        this.loading.set(false);
        this.existing.set(auction);

        if (auction.sellerUsername !== this.auth.username() && !this.auth.isAdmin()) {
          this.blockedReason.set('You can only edit auctions you created.');
          return;
        }
        if (auction.status !== 'SCHEDULED') {
          this.blockedReason.set(
            'Only auctions that have not opened yet can be edited. ' +
              'Once bidding starts, the terms are fixed for everyone who has bid.',
          );
          return;
        }

        this.blockedReason.set(null);
        this.fill(auction);
      },
      error: () => {
        this.loading.set(false);
        this.blockedReason.set('That auction could not be loaded.');
      },
    });
  }

  private fill(auction: AuctionResponse): void {
    this.form.controls.auctionType.setValue(auction.auctionType);

    this.form.patchValue({
      title: auction.title,
      description: auction.description ?? '',
      category: auction.category,
      startingPrice: auction.startingPrice,
      minIncrement: auction.minIncrement ?? null,
      startTime: isoToLocalInput(auction.startTime),
      endTime: isoToLocalInput(auction.endTime),
    });

    this.form.markAsPristine();
  }
}
