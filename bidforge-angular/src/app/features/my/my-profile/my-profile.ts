import { DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { ROLE_ADMIN } from '../../../core/models/enums';
import { AuthService } from '../../../core/services/auth.service';

const ROLE_DESCRIPTIONS: Record<string, { label: string; icon: string; blurb: string }> = {
  ROLE_USER: {
    label: 'Member',
    icon: 'person',
    blurb: 'Can browse, bid, and list auctions of their own.',
  },
  ROLE_ADMIN: {
    label: 'Administrator',
    icon: 'shield',
    blurb: 'Can also manage every user and auction, and read the audit trail.',
  },
};

@Component({
  selector: 'app-my-profile',
  imports: [RouterLink, DatePipe, MatCardModule, MatButtonModule, MatIconModule, MatDividerModule],
  templateUrl: './my-profile.html',
  styleUrl: './my-profile.scss',
})
export class MyProfile {
  protected readonly auth = inject(AuthService);

  protected readonly user = this.auth.currentUser;

  protected readonly fullName = computed(() => {
    const current = this.user();
    return current ? `${current.firstName} ${current.lastName}` : '';
  });

  protected readonly roles = computed(() => {
    const current = this.user();
    if (!current) return [];

    return [...current.roles]
      .sort((a, b) => (a === ROLE_ADMIN ? -1 : b === ROLE_ADMIN ? 1 : 0))
      .map((role) => ({
        raw: role,
        ...(ROLE_DESCRIPTIONS[role] ?? { label: role, icon: 'badge', blurb: '' }),
      }));
  });

  protected signOut(): void {
    this.auth.logout();
  }
}
