import { HttpContext, HttpParams } from '@angular/common/http';
import { SKIP_ERROR_TOAST } from './http-context.tokens';

export function toHttpParams(values: Record<string, unknown>): HttpParams {
  let params = new HttpParams();

  for (const [key, value] of Object.entries(values)) {
    if (value === null || value === undefined || value === '') {
      continue;
    }
    params = params.set(key, String(value));
  }

  return params;
}

export function skipErrorToast(): HttpContext {
  return new HttpContext().set(SKIP_ERROR_TOAST, true);
}
