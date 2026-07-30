import { FormControl, FormGroup } from '@angular/forms';
import {
  endAfterStartValidator,
  futureDateTimeValidator,
  twoDecimalsValidator,
} from './validators';

function control(value: unknown) {
  return new FormControl(value);
}

describe('twoDecimalsValidator', () => {
  it('accepts values with no decimal part', () => {
    expect(twoDecimalsValidator(control(1200))).toBeNull();
    expect(twoDecimalsValidator(control('1200'))).toBeNull();
  });

  it('accepts one and two decimal places', () => {
    expect(twoDecimalsValidator(control(10.5))).toBeNull();
    expect(twoDecimalsValidator(control(10.55))).toBeNull();
  });

  it('rejects three or more decimal places', () => {
    expect(twoDecimalsValidator(control(10.005))).toEqual({ twoDecimals: true });
    expect(twoDecimalsValidator(control(0.1234))).toEqual({ twoDecimals: true });
  });

  it('leaves empty values to the required validator', () => {
    expect(twoDecimalsValidator(control(null))).toBeNull();
    expect(twoDecimalsValidator(control(undefined))).toBeNull();
    expect(twoDecimalsValidator(control(''))).toBeNull();
  });

  it('does not trip over binary floating point', () => {
    expect(twoDecimalsValidator(control(0.1 + 0.2))).toEqual({ twoDecimals: true });
    expect(twoDecimalsValidator(control(0.3))).toBeNull();
  });
});

describe('futureDateTimeValidator', () => {
  function localInput(offsetMs: number): string {
    const date = new Date(Date.now() + offsetMs);
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
    return local.toISOString().slice(0, 16);
  }

  it('accepts a time in the future', () => {
    expect(futureDateTimeValidator(control(localInput(60 * 60_000)))).toBeNull();
  });

  it('rejects a time in the past', () => {
    expect(futureDateTimeValidator(control(localInput(-24 * 60 * 60_000)))).toEqual({
      pastDate: true,
    });
  });

  it('allows a one-minute grace period around now', () => {
    vi.useFakeTimers();
    try {
      vi.setSystemTime(new Date('2026-08-01T12:00:30'));
      expect(futureDateTimeValidator(control('2026-08-01T12:00'))).toBeNull();
    } finally {
      vi.useRealTimers();
    }
  });

  it('stops forgiving once the grace period has passed', () => {
    vi.useFakeTimers();
    try {
      vi.setSystemTime(new Date('2026-08-01T12:02:00'));
      expect(futureDateTimeValidator(control('2026-08-01T12:00'))).toEqual({
        pastDate: true,
      });
    } finally {
      vi.useRealTimers();
    }
  });

  it('flags an unparseable value distinctly from a past one', () => {
    expect(futureDateTimeValidator(control('not-a-date'))).toEqual({ invalidDate: true });
  });

  it('leaves empty values alone', () => {
    expect(futureDateTimeValidator(control(''))).toBeNull();
    expect(futureDateTimeValidator(control(null))).toBeNull();
  });
});

describe('endAfterStartValidator', () => {
  const validate = endAfterStartValidator('startTime', 'endTime');

  function group(startTime: string | null, endTime: string | null) {
    return new FormGroup({
      startTime: new FormControl(startTime),
      endTime: new FormControl(endTime),
    });
  }

  it('accepts an end strictly after the start', () => {
    expect(validate(group('2026-08-01T10:00', '2026-08-08T10:00'))).toBeNull();
  });

  it('rejects an end before the start', () => {
    expect(validate(group('2026-08-08T10:00', '2026-08-01T10:00'))).toEqual({
      endBeforeStart: true,
    });
  });

  it('rejects an end equal to the start — a zero-length auction is not valid', () => {
    expect(validate(group('2026-08-01T10:00', '2026-08-01T10:00'))).toEqual({
      endBeforeStart: true,
    });
  });

  it('stays quiet until both fields have values', () => {
    expect(validate(group('2026-08-01T10:00', null))).toBeNull();
    expect(validate(group(null, '2026-08-01T10:00'))).toBeNull();
    expect(validate(group(null, null))).toBeNull();
  });

  it('stays quiet on unparseable input, leaving that to the field validator', () => {
    expect(validate(group('nonsense', '2026-08-01T10:00'))).toBeNull();
  });

  it('attaches its error to the group, not to either control', () => {
    const form = group('2026-08-08T10:00', '2026-08-01T10:00');
    form.setValidators([validate]);
    form.updateValueAndValidity();

    expect(form.hasError('endBeforeStart')).toBe(true);
    expect(form.controls.endTime.errors).toBeNull();
    expect(form.controls.startTime.errors).toBeNull();
  });
});
