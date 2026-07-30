import { Injectable, effect, signal } from '@angular/core';

const THEME_STORAGE_KEY = 'bidforge.theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly _isDark = signal<boolean>(initialPreference());

  readonly isDark = this._isDark.asReadonly();

  constructor() {
    effect(() => {
      const dark = this._isDark();
      document.documentElement.classList.toggle('dark', dark);
      localStorage.setItem(THEME_STORAGE_KEY, dark ? 'dark' : 'light');
    });
  }

  toggle(): void {
    this._isDark.update((dark) => !dark);
  }
}

function initialPreference(): boolean {
  const stored = localStorage.getItem(THEME_STORAGE_KEY);
  if (stored === 'dark') return true;
  if (stored === 'light') return false;
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
}
