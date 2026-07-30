import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { SKIP_ERROR_TOAST } from '../http/http-context.tokens';
import { ApiError } from '../models/api-error.model';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const notifications = inject(NotificationService);
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      const silent = request.context.get(SKIP_ERROR_TOAST);

      const isAuthCall = request.url.includes('/auth/');

      if (error.status === 0) {
        if (!silent) {
          notifications.error(
            'Cannot reach the server. Make sure the backend is running on http://localhost:8080.',
          );
        }
        return throwError(() => error);
      }

      const apiError = error.error as ApiError | null;
      const message = apiError?.message ?? 'Something went wrong. Please try again.';

      if (error.status === 401 && !isAuthCall) {
        auth.clearSession();
        if (!silent) {
          notifications.error('Your session has ended. Please sign in again.');
        }
        void router.navigate(['/login'], {
          queryParams: { returnUrl: router.url },
        });
        return throwError(() => error);
      }

      if (!silent && apiError?.fieldErrors?.length) {
        const details = apiError.fieldErrors
          .map((fieldError) => `${fieldError.field}: ${fieldError.message}`)
          .join(' · ');
        notifications.error(details);
        return throwError(() => error);
      }

      if (!silent) {
        notifications.error(message);
      }

      return throwError(() => error);
    }),
  );
};
