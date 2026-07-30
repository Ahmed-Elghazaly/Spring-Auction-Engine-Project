import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Component, computed, input, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { interval } from 'rxjs';
import { AuctionStatus } from '../../../core/models/enums';

@Component({
  selector: 'app-countdown',
  imports: [MatIconModule],
  template: `
    <span class="bf-countdown" [class.bf-countdown--urgent]="isUrgent()">
      <mat-icon class="bf-countdown__icon">{{ icon() }}</mat-icon>
      <span class="bf-tabular">{{ text() }}</span>
    </span>
  `,
  styleUrl: './countdown.scss',
})
export class Countdown {
  readonly startTime = input.required<string>();
  readonly endTime = input.required<string>();
  readonly status = input.required<AuctionStatus>();

  private readonly now = signal(Date.now());

  constructor() {
    interval(1000)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.now.set(Date.now()));
  }

  private readonly target = computed<number | null>(() => {
    switch (this.status()) {
      case 'SCHEDULED':
        return new Date(this.startTime()).getTime();
      case 'OPEN':
        return new Date(this.endTime()).getTime();
      default:
        return null;
    }
  });

  private readonly millisLeft = computed(() => {
    const target = this.target();
    if (target === null) return 0;
    return Math.max(0, target - this.now());
  });

  protected readonly icon = computed(() => {
    switch (this.status()) {
      case 'SCHEDULED':
        return 'schedule';
      case 'OPEN':
        return 'timer';
      case 'CANCELLED':
        return 'block';
      default:
        return 'flag';
    }
  });

  protected readonly isUrgent = computed(
    () => this.status() === 'OPEN' && this.millisLeft() > 0 && this.millisLeft() < 3_600_000,
  );

  protected readonly text = computed(() => {
    const status = this.status();

    if (status === 'CLOSED') return 'Ended';
    if (status === 'CANCELLED') return 'Cancelled';

    const millis = this.millisLeft();

    if (millis <= 0) {
      return status === 'SCHEDULED' ? 'Starting…' : 'Closing…';
    }

    const prefix = status === 'SCHEDULED' ? 'Starts in' : 'Closes in';
    return `${prefix} ${formatDuration(millis)}`;
  });
}

function formatDuration(millis: number): string {
  const totalSeconds = Math.floor(millis / 1000);
  const days = Math.floor(totalSeconds / 86_400);
  const hours = Math.floor((totalSeconds % 86_400) / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  if (days > 0) return `${days}d ${pad(hours)}h`;
  if (hours > 0) return `${hours}h ${pad(minutes)}m`;
  if (minutes > 0) return `${pad(minutes)}:${pad(seconds)}`;
  return `${seconds}s`;
}

function pad(value: number): string {
  return value.toString().padStart(2, '0');
}
