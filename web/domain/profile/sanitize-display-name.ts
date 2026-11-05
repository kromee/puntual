const MAX_DISPLAY_NAME_LENGTH = 80;

export function sanitizeDisplayName(raw: string): string {
  return raw
    .trim()
    .replace(/\s+Puntual\s*$/i, "")
    .replace(/^Puntual\s+/i, "")
    .trim()
    .slice(0, MAX_DISPLAY_NAME_LENGTH);
}
