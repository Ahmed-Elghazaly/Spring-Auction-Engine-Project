import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-empty-state',
  imports: [MatIconModule],
  template: `
    <div class="bf-empty">
      <mat-icon class="bf-empty__icon">{{ icon() }}</mat-icon>
      <p class="bf-empty__title">{{ title() }}</p>
      @if (message()) {
        <p class="bf-empty__message bf-muted">{{ message() }}</p>
      }
      <div class="bf-empty__action">
        <ng-content />
      </div>
    </div>
  `,
  styleUrl: './empty-state.scss',
})
export class EmptyState {
  readonly icon = input<string>('inbox');
  readonly title = input.required<string>();
  readonly message = input<string>('');
}
