import { isoToLocalInput, localInputFromNow, localInputToIso } from './datetime.util';

describe('localInputToIso', () => {
  it('interprets the input as local time and returns an instant', () => {
    const iso = localInputToIso('2026-08-01T14:30');
    expect(iso).not.toBeNull();

    expect(new Date(iso!).getTime()).toBe(new Date('2026-08-01T14:30').getTime());
  });

  it('produces a UTC string ending in Z', () => {
    expect(localInputToIso('2026-08-01T14:30')).toMatch(/Z$/);
  });

  it('returns null for empty or missing input', () => {
    expect(localInputToIso('')).toBeNull();
    expect(localInputToIso(null)).toBeNull();
    expect(localInputToIso(undefined)).toBeNull();
  });

  it('returns null rather than "Invalid Date" for nonsense', () => {
    expect(localInputToIso('not-a-date')).toBeNull();
  });
});

describe('isoToLocalInput', () => {
  it('produces exactly the shape a datetime-local input accepts', () => {
    const value = isoToLocalInput('2026-08-01T11:30:00.000Z');
    expect(value).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/);
  });

  it('returns an empty string for empty or invalid input', () => {
    expect(isoToLocalInput('')).toBe('');
    expect(isoToLocalInput(null)).toBe('');
    expect(isoToLocalInput(undefined)).toBe('');
    expect(isoToLocalInput('not-a-date')).toBe('');
  });
});

describe('the round trip', () => {
  it('preserves the instant through local -> ISO -> local', () => {
    for (const input of [
      '2026-01-15T09:05',
      '2026-06-30T23:59',
      '2026-08-01T14:30',
      '2026-12-31T00:00',
    ]) {
      const iso = localInputToIso(input);
      expect(isoToLocalInput(iso)).toBe(input);
    }
  });

  it('preserves the instant through ISO -> local -> ISO', () => {
    const iso = '2026-08-01T11:30:00.000Z';
    const back = localInputToIso(isoToLocalInput(iso));

    expect(new Date(back!).getTime()).toBe(new Date(iso).getTime());
  });

  it('survives a date on the far side of a DST boundary', () => {
    for (const input of ['2026-01-15T12:00', '2026-07-15T12:00']) {
      expect(isoToLocalInput(localInputToIso(input))).toBe(input);
    }
  });
});

describe('localInputFromNow', () => {
  it('returns the datetime-local shape', () => {
    expect(localInputFromNow(60)).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/);
  });

  it('offsets by the requested number of minutes', () => {
    const now = localInputToIso(localInputFromNow(0))!;
    const inAnHour = localInputToIso(localInputFromNow(60))!;

    const deltaMinutes = (new Date(inAnHour).getTime() - new Date(now).getTime()) / 60_000;

    expect(deltaMinutes).toBeGreaterThanOrEqual(59);
    expect(deltaMinutes).toBeLessThanOrEqual(61);
  });

  it('produces the defaults the auction form uses', () => {
    const start = localInputToIso(localInputFromNow(60))!;
    const end = localInputToIso(localInputFromNow(60 * 24 * 7))!;
    expect(new Date(end).getTime()).toBeGreaterThan(new Date(start).getTime());
  });
});
