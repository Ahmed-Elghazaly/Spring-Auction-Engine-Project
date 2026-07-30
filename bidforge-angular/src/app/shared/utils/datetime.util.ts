export function localInputToIso(value: string | null | undefined): string | null {
  if (!value) return null;
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) return null;
  return date.toISOString();
}

export function isoToLocalInput(iso: string | null | undefined): string {
  if (!iso) return '';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '';
  const offsetMs = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
}

export function localInputFromNow(minutes: number): string {
  return isoToLocalInput(new Date(Date.now() + minutes * 60_000).toISOString());
}
