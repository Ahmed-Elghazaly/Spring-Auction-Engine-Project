import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forbidden',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  template: `
    <div class="bf-container bf-page bf-error">
      <mat-icon class="bf-error__icon">lock</mat-icon>
      <h1 class="bf-error__title">Access denied</h1>
      <p class="bf-muted">
        @if (auth.isLoggedIn()) {
          You are signed in as <strong>{{ auth.username() }}</strong
          >, but this area is restricted to administrators.
        } @else {
          You need to sign in to view this page.
        }
      </p>
      <div class="bf-error__actions">
        <a mat-flat-button routerLink="/">Go to home</a>
        @if (!auth.isLoggedIn()) {
          <a mat-stroked-button routerLink="/login">Sign in</a>
        }
      </div>
    </div>
  `,
  styleUrl: '../not-found/error-page.scss',
})
export class Forbidden {
  protected readonly auth = inject(AuthService);
}
