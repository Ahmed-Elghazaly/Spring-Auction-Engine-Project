import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ROLE_ADMIN } from '../../../core/models/enums';
import { Page, emptyPage } from '../../../core/models/page.model';
import { UserResponse } from '../../../core/models/user.model';
import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';

@Component({
  selector: 'app-admin-users',
  imports: [
    DatePipe,
    MatTableModule,
    MatPaginatorModule,
    MatSlideToggleModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    EmptyState,
  ],
  templateUrl: './admin-users.html',
  styleUrl: '../admin-page.scss',
})
export class AdminUsers {
  private readonly adminService = inject(AdminService);
  private readonly notifications = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly result = signal<Page<UserResponse>>(emptyPage(15));
  protected readonly loading = signal(true);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(15);

  protected readonly busyId = signal<number | null>(null);

  protected readonly columns = ['user', 'email', 'roles', 'joined', 'status'] as const;

  protected readonly disabledCount = computed(
    () => this.result().content.filter((user) => !user.enabled).length,
  );

  constructor() {
    this.load();
  }

  protected isSelf(user: UserResponse): boolean {
    return user.username === this.auth.username();
  }

  protected isAdmin(user: UserResponse): boolean {
    return user.roles.includes(ROLE_ADMIN);
  }

  protected fullName(user: UserResponse): string {
    return `${user.firstName} ${user.lastName}`;
  }

  protected initials(user: UserResponse): string {
    return (user.firstName.charAt(0) + user.lastName.charAt(0)).toUpperCase();
  }

  protected toggle(user: UserResponse, enabled: boolean): void {
    if (enabled) {
      this.apply(user, true);
      return;
    }

    this.dialog
      .open(ConfirmDialog, {
        width: '460px',
        data: {
          title: `Disable ${user.username}?`,
          message:
            'They will be signed out the moment they do anything else — the server ' +
            'checks the account on every single request, so their existing token ' +
            'stops working immediately. Their auctions and bids are left untouched.',
          confirmLabel: 'Disable account',
          danger: true,
        },
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) this.apply(user, false);
      });
  }

  protected onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  private apply(user: UserResponse, enabled: boolean): void {
    this.busyId.set(user.id);

    this.adminService.updateUserStatus(user.id, enabled).subscribe({
      next: (updated) => {
        this.busyId.set(null);
        this.notifications.success(
          enabled ? `${updated.username} can sign in again.` : `${updated.username} is disabled.`,
        );

        this.result.update((page) => ({
          ...page,
          content: page.content.map((row) => (row.id === updated.id ? updated : row)),
        }));
      },

      error: () => this.busyId.set(null),
    });
  }

  private load(): void {
    this.loading.set(true);

    this.adminService
      .listUsers({ page: this.pageIndex(), size: this.pageSize(), sort: 'createdAt,desc' })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.result.set(page);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }
}
