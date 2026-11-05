import { ATTENDANCE_TIME_ZONE } from "@/domain/attendance/attendance-clock";

export function formatWorkDate(isoDate: string): string {
  const [year, month, day] = isoDate.split("-").map(Number);
  return new Intl.DateTimeFormat("es-MX", {
    day: "numeric",
    month: "long",
    timeZone: "UTC",
  }).format(new Date(Date.UTC(year, month - 1, day)));
}

export function formatCheckInTime(isoTimestamp: string): string {
  return new Intl.DateTimeFormat("es-MX", {
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
    timeZone: ATTENDANCE_TIME_ZONE,
  }).format(new Date(isoTimestamp));
}

export function formatDelay(minutes: number): string {
  if (minutes <= 0) {
    return "A tiempo";
  }
  return `${minutes} min`;
}

export function clockFromTimestamp(isoTimestamp: string): string {
  return formatCheckInTime(isoTimestamp);
}
