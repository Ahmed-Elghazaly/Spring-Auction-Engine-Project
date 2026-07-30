import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { combineLatest, debounceTime, distinctUntilChanged, startWith } from 'rxjs';
import { AuditEventResponse } from '../../../core/models/audit.model';
import { AUDIT_ACTION_LABELS, AuditAction } from '../../../core/models/enums';
import { Page, emptyPage } from '../../../core/models/page.model';
import { AdminService } from '../../../core/services/admin.service';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';

const ENTITY_TYPES = ['AUCTION', 'USER'] as const;

const SYSTEM_ACTOR = 'SYSTEM';

const ACTION_STYLE: Record<AuditAction, { icon: string; tone: string }> = {
  USER_REGISTERED: { icon: 'person_add', tone: 'neutral' },
  USER_STATUS_CHANGED: { icon: 'manage_accounts', tone: 'warn' },
  AUCTION_CREATED: { icon: 'add_circle', tone: 'neutral' },
  AUCTION_UPDATED: { icon: 'edit', tone: 'neutral' },
  AUCTION_OPENED: { icon: 'play_arrow', tone: 'good' },
  AUCTION_CLOSED: { icon: 'flag', tone: 'neutral' },
  AUCTION_CANCELLED: { icon: 'block', tone: 'warn' },
  BID_PLACED: { icon: 'gavel', tone: 'good' },
  WINNER_SELECTED: { icon: 'emoji_events', tone: 'gold' },
};

@Component({
  selector: 'app-admin-audit',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    DatePipe,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    EmptyState,
  ],
  templateUrl: './admin-audit.html',
  styleUrl: '../admin-page.scss',
})
export class AdminAudit {
  private readonly adminService = inject(AdminService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly entityTypes = ENTITY_TYPES;
  protected readonly actionLabels = AUDIT_ACTION_LABELS;

  protected readonly result = signal<Page<AuditEventResponse>>(emptyPage(25));
  protected readonly loading = signal(true);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(25);

  protected readonly columns = ['when', 'actor', 'action', 'entity', 'details'] as const;

  protected readonly entityType = signal<string | undefined>(undefined);

  protected readonly actorControl = new FormControl('', { nonNullable: true });
  protected readonly entityIdControl = new FormControl<number | null>(null);

  protected readonly systemCount = computed(
    () => this.result().content.filter((event) => event.actor === SYSTEM_ACTOR).length,
  );

  constructor() {
    combineLatest([
      this.actorControl.valueChanges.pipe(startWith('')),
      this.entityIdControl.valueChanges.pipe(startWith(null)),
    ])
      .pipe(debounceTime(350), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => {
        this.pageIndex.set(0);
        this.load();
      });
  }

  protected isSystem(event: AuditEventResponse): boolean {
    return event.actor === SYSTEM_ACTOR;
  }

  protected label(event: AuditEventResponse): string {
    return AUDIT_ACTION_LABELS[event.action] ?? event.action;
  }

  protected style(event: AuditEventResponse): { icon: string; tone: string } {
    return ACTION_STYLE[event.action] ?? { icon: 'history', tone: 'neutral' };
  }

  protected linksToAuction(event: AuditEventResponse): boolean {
    return event.entityType === 'AUCTION';
  }

  protected onFilterChange(): void {
    this.pageIndex.set(0);
    this.load();
  }

  protected onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  protected hasActiveFilters(): boolean {
    return (
      this.entityType() !== undefined ||
      this.actorControl.value.trim().length > 0 ||
      this.entityIdControl.value !== null
    );
  }

  protected clearFilters(): void {
    this.entityType.set(undefined);
    this.actorControl.setValue('', { emitEvent: false });
    this.entityIdControl.setValue(null, { emitEvent: false });
    this.pageIndex.set(0);
    this.load();
  }

  protected showSystemOnly(): void {
    this.actorControl.setValue(SYSTEM_ACTOR);
  }

  protected traceEntity(event: AuditEventResponse): void {
    this.entityType.set(event.entityType);
    this.entityIdControl.setValue(event.entityId);
  }

  private load(): void {
    this.loading.set(true);

    this.adminService
      .auditEvents(
        {
          entityType: this.entityType(),
          entityId: this.entityIdControl.value ?? undefined,
          actor: this.actorControl.value.trim() || undefined,
        },
        { page: this.pageIndex(), size: this.pageSize(), sort: 'createdAt,desc' },
      )
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
