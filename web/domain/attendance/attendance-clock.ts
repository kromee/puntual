export const ATTENDANCE_TIME_ZONE = "America/Mexico_City";
export const FIRST_PERIOD_START = "2025-01-01";

export function attendanceToday(now = new Date()): string {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: ATTENDANCE_TIME_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "numeric",
  }).format(now);
}

export function isWorkday(isoDate: string): boolean {
  const [year, month, day] = isoDate.split("-").map(Number);
  const weekDay = new Date(Date.UTC(year, month - 1, day)).getUTCDay();
  return weekDay >= 1 && weekDay <= 5;
}
