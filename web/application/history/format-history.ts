export function formatMonthTitle(year: number, month: number): string {
  const label = new Intl.DateTimeFormat("es-MX", {
    month: "long",
    year: "numeric",
    timeZone: "UTC",
  }).format(new Date(Date.UTC(year, month - 1, 1)));
  return label.charAt(0).toLocaleUpperCase("es-MX") + label.slice(1);
}

export function formatShortMonth(year: number, month: number): string {
  const label = new Intl.DateTimeFormat("es-MX", {
    month: "long",
    timeZone: "UTC",
  }).format(new Date(Date.UTC(year, month - 1, 1)));
  return label.charAt(0).toLocaleUpperCase("es-MX") + label.slice(1);
}

export function formatHoursMinutes(totalMinutes: number): string {
  const safe = Math.max(0, totalMinutes);
  const hours = Math.floor(safe / 60);
  const minutes = safe % 60;
  if (hours > 0 && minutes > 0) {
    return `${hours} h ${minutes} min`;
  }
  if (hours > 0) {
    return `${hours} h`;
  }
  if (minutes > 0) {
    return `${minutes} min`;
  }
  return "0 min";
}
