import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  template: `
    <div class="bf-container bf-page bf-error">
      <mat-icon class="bf-error__icon">travel_explore</mat-icon>
      <h1 class="bf-error__title">Page not found</h1>
      <p class="bf-muted">The page you are looking for doesn't exist, or it may have been moved.</p>
      <div class="bf-error__actions">
        <a mat-flat-button routerLink="/">Go to home</a>
        <a mat-stroked-button routerLink="/auctions">Browse auctions</a>
      </div>
    </div>
  `,
  styleUrl: './error-page.scss',
})
export class NotFound {}
