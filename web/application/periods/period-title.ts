export function formatPeriodDate(isoDate: string): string {
  const [year, month, day] = isoDate.split("-").map(Number);
  return new Intl.DateTimeFormat("es-MX", {
    day: "numeric",
    month: "short",
    year: "numeric",
    timeZone: "UTC",
  }).format(new Date(Date.UTC(year, month - 1, day)));
}

export function defaultPeriodTitle(startDate: string, endDate: string | null): string {
  const end = endDate ? formatPeriodDate(endDate) : "actualidad";
  return `${formatPeriodDate(startDate)} – ${end}`;
}
