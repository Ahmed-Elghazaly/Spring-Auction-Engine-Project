import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, switchMap, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { skipErrorToast } from '../http/http-params.util';
import { ROLE_ADMIN } from '../models/enums';
import { AuthResponse, LoginRequest, RegisterRequest, UserResponse } from '../models/user.model';

const TOKEN_STORAGE_KEY = 'bidforge.token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly _token = signal<string | null>(localStorage.getItem(TOKEN_STORAGE_KEY));

  private readonly _currentUser = signal<UserResponse | null>(null);

  readonly token = this._token.asReadonly();
  readonly currentUser = this._currentUser.asReadonly();

  readonly isLoggedIn = computed(() => this._token() !== null);
  readonly isAdmin = computed(() => this._currentUser()?.roles.includes(ROLE_ADMIN) ?? false);
  readonly username = computed(() => this._currentUser()?.username ?? null);

  readonly initials = computed(() => {
    const user = this._currentUser();
    if (!user) return '';
    return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
  });

  login(request: LoginRequest): Observable<UserResponse> {
    return this.http.post<AuthResponse>(`${environment.apiBaseUrl}/auth/login`, request).pipe(
      tap((auth) => this.storeToken(auth.token)),
      switchMap(() => this.loadCurrentUser()),
    );
  }

  register(request: RegisterRequest): Observable<UserResponse> {
    return this.http
      .post<UserResponse>(`${environment.apiBaseUrl}/auth/register`, request)
      .pipe(
        switchMap(() => this.login({ username: request.username, password: request.password })),
      );
  }

  loadCurrentUser(): Observable<UserResponse> {
    return this.http
      .get<UserResponse>(`${environment.apiBaseUrl}/users/me`)
      .pipe(tap((user) => this._currentUser.set(user)));
  }

  restoreSession(): Observable<UserResponse | null> {
    if (!this._token()) {
      return of(null);
    }

    return this.http
      .get<UserResponse>(`${environment.apiBaseUrl}/users/me`, { context: skipErrorToast() })
      .pipe(
        tap((user) => this._currentUser.set(user)),
        map((user) => user as UserResponse | null),
        catchError(() => {
          this.clearSession();
          return of(null);
        }),
      );
  }

  logout(): void {
    this.clearSession();
  }

  clearSession(): void {
    this._token.set(null);
    this._currentUser.set(null);
    localStorage.removeItem(TOKEN_STORAGE_KEY);
  }

  private storeToken(token: string): void {
    this._token.set(token);
    localStorage.setItem(TOKEN_STORAGE_KEY, token);
  }
}
