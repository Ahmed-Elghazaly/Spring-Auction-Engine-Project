import { Component, computed, input } from '@angular/core';
import { AUCTION_STATUS_LABELS, AuctionStatus } from '../../../core/models/enums';

@Component({
  selector: 'app-status-chip',
  template: `<span class="bf-chip" [class]="'bf-chip--' + status().toLowerCase()">{{
    label()
  }}</span>`,
  styleUrl: './status-chip.scss',
})
export class StatusChip {
  readonly status = input.required<AuctionStatus>();

  protected readonly label = computed(() => AUCTION_STATUS_LABELS[this.status()]);
}
