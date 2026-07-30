import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-skeleton-grid',
  template: `
    <div class="bf-grid" aria-hidden="true">
      @for (placeholder of placeholders(); track placeholder) {
        <div class="bf-skeleton-card">
          <div class="bf-skeleton bf-skeleton-card__band"></div>
          <div class="bf-skeleton bf-skeleton-card__line bf-skeleton-card__line--title"></div>
          <div class="bf-skeleton bf-skeleton-card__line bf-skeleton-card__line--short"></div>
          <div class="bf-skeleton bf-skeleton-card__line bf-skeleton-card__line--price"></div>
        </div>
      }
    </div>
  `,
  styleUrl: './skeleton-grid.scss',
})
export class SkeletonGrid {
  readonly count = input<number>(8);

  protected readonly placeholders = computed(() =>
    Array.from({ length: this.count() }, (_, index) => index),
  );
}
