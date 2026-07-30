import { Component, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

const DEMO_ACCOUNTS = [
  { username: 'admin', password: 'Admin@123', role: 'Administrator' },
  { username: 'sara', password: 'Password@123', role: 'Seller' },
  { username: 'omar', password: 'Password@123', role: 'Seller' },
  { username: 'layla', password: 'Password@123', role: 'Bidder' },
  { username: 'youssef', password: 'Password@123', role: 'Bidder' },
];

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './login.html',
  styleUrl: '../auth-page.scss',
})
export class Login {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);

  readonly returnUrl = input<string | undefined>(undefined);

  private readonly targetUrl = computed(() => this.returnUrl() || '/');

  protected readonly demoAccounts = DEMO_ACCOUNTS;

  protected readonly submitting = signal(false);

  protected readonly hidePassword = signal(true);

  protected readonly form = this.formBuilder.nonNullable.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]],
  });

  protected readonly canSubmit = computed(() => !this.submitting());

  protected fillDemoAccount(account: (typeof DEMO_ACCOUNTS)[number]): void {
    this.form.setValue({ username: account.username, password: account.password });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    this.auth.login(this.form.getRawValue()).subscribe({
      next: (user) => {
        this.notifications.success(`Welcome back, ${user.firstName}!`);
        void this.router.navigateByUrl(this.targetUrl());
      },
      error: () => this.submitting.set(false),
    });
  }
}
