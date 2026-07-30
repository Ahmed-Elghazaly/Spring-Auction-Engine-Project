import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { EMPTY, Observable, switchMap, tap } from 'rxjs';
import {
  ConfirmDialog,
  ConfirmDialogData,
} from '../../shared/components/confirm-dialog/confirm-dialog';
import { AuctionResponse } from '../models/auction.model';
import { AuctionStatus } from '../models/enums';
import { AuctionService } from './auction.service';
import { NotificationService } from './notification.service';

@Injectable({ providedIn: 'root' })
export class AuctionLifecycleService {
  private readonly dialog = inject(MatDialog);
  private readonly auctions = inject(AuctionService);
  private readonly notifications = inject(NotificationService);

  open(id: number): Observable<AuctionResponse> {
    return this.confirmThen(
      {
        title: 'Open this auction now?',
        message:
          'Bidding will start immediately, before the scheduled start time. ' +
          'Once open, the auction can no longer be edited.',
        confirmLabel: 'Open auction',
      },
      () => this.auctions.open(id),
      'Auction opened — bidding is now live.',
    );
  }

  close(id: number): Observable<AuctionResponse> {
    return this.confirmThen(
      {
        title: 'Close this auction now?',
        message:
          'The winner will be decided immediately from the bids placed so far, ' +
          'and the result is permanent. This cannot be undone.',
        confirmLabel: 'Close and pick winner',
        danger: true,
      },
      () => this.auctions.close(id),
      'Auction closed — the winner has been decided.',
    );
  }

  cancel(id: number): Observable<AuctionResponse> {
    return this.confirmThen(
      {
        title: 'Cancel this auction?',
        message:
          'The auction will end with no winner. Existing bids stay on record, ' +
          'but nothing can be bid afterwards. This cannot be undone.',
        confirmLabel: 'Cancel auction',
        danger: true,
      },
      () => this.auctions.cancel(id),
      'Auction cancelled.',
    );
  }

  allowedActions(
    status: AuctionStatus,
    isAdmin: boolean,
  ): { canEdit: boolean; canOpen: boolean; canClose: boolean; canCancel: boolean } {
    return {
      canEdit: status === 'SCHEDULED',
      canOpen: status === 'SCHEDULED',
      canClose: status === 'OPEN',
      canCancel: status === 'SCHEDULED' || (status === 'OPEN' && isAdmin),
    };
  }

  private confirmThen(
    data: ConfirmDialogData,
    action: () => Observable<AuctionResponse>,
    successMessage: string,
  ): Observable<AuctionResponse> {
    return this.dialog
      .open(ConfirmDialog, { data, width: '440px' })
      .afterClosed()
      .pipe(
        switchMap((confirmed) => (confirmed ? action() : EMPTY)),
        tap(() => this.notifications.success(successMessage)),
      );
  }
}
