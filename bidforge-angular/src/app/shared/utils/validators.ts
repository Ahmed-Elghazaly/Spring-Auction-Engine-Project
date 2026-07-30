import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function twoDecimalsValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value;

  if (value === null || value === undefined || value === '') return null;

  const text = String(value);
  const dot = text.indexOf('.');
  if (dot === -1) return null;

  return text.length - dot - 1 > 2 ? { twoDecimals: true } : null;
}

export function futureDateTimeValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value as string | null;
  if (!value) return null;

  const chosen = new Date(value);
  if (Number.isNaN(chosen.getTime())) return { invalidDate: true };

  return chosen.getTime() <= Date.now() - 60_000 ? { pastDate: true } : null;
}

export function endAfterStartValidator(startField: string, endField: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const start = group.get(startField)?.value as string | null;
    const end = group.get(endField)?.value as string | null;
    if (!start || !end) return null;

    const startMs = new Date(start).getTime();
    const endMs = new Date(end).getTime();
    if (Number.isNaN(startMs) || Number.isNaN(endMs)) return null;

    return endMs > startMs ? null : { endBeforeStart: true };
  };
}
