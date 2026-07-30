import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const token = inject(AuthService).token();

  const isApiCall = request.url.startsWith(environment.apiBaseUrl);
  if (!token || !isApiCall) {
    return next(request);
  }

  const authorised = request.clone({
    setHeaders: { Authorization: `Bearer ${token}` },
  });

  return next(authorised);
};
