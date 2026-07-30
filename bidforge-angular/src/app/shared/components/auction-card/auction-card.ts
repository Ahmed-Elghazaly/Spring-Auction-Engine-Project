import { CurrencyPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { AuctionSummaryResponse } from '../../../core/models/auction.model';
import { AUCTION_CATEGORY_LABELS, AUCTION_TYPE_LABELS } from '../../../core/models/enums';
import { categoryVisual } from '../../utils/category-visuals';
import { Countdown } from '../countdown/countdown';
import { StatusChip } from '../status-chip/status-chip';

@Component({
  selector: 'app-auction-card',
  imports: [RouterLink, CurrencyPipe, MatCardModule, MatIconModule, StatusChip, Countdown],
  templateUrl: './auction-card.html',
  styleUrl: './auction-card.scss',
})
export class AuctionCard {
  readonly auction = input.required<AuctionSummaryResponse>();

  protected readonly visual = computed(() => categoryVisual(this.auction().category));

  protected readonly categoryLabel = computed(
    () => AUCTION_CATEGORY_LABELS[this.auction().category],
  );

  protected readonly typeLabel = computed(() => AUCTION_TYPE_LABELS[this.auction().auctionType]);

  protected readonly isSealed = computed(() => this.auction().auctionType === 'SEALED_BID');

  protected readonly hasBids = computed(() => (this.auction().currentHighestBid ?? null) !== null);

  protected readonly displayPrice = computed(
    () => this.auction().currentHighestBid ?? this.auction().startingPrice,
  );

  protected readonly priceLabel = computed(() => {
    if (this.isSealed()) return 'Starting price';
    return this.hasBids() ? 'Current bid' : 'Starting price';
  });
}
