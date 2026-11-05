import type { AttendancePeriod } from "@/domain/attendance/attendance-period";

export const FIRST_HISTORY_YEAR = 2025;

export type YearMonth = {
  year: number;
  month: number;
};

export function yearMonthOf(isoDate: string): YearMonth {
  const [year, month] = isoDate.split("-").map(Number);
  return { year, month };
}

export function compareYearMonth(left: YearMonth, right: YearMonth): number {
  return left.year === right.year ? left.month - right.month : left.year - right.year;
}

export function isoDate(year: number, month: number, day: number): string {
  return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

export function endOfMonth(year: number, month: number): string {
  const lastDay = new Date(Date.UTC(year, month, 0)).getUTCDate();
  return isoDate(year, month, lastDay);
}

export function addDays(iso: string, days: number): string {
  const [year, month, day] = iso.split("-").map(Number);
  const date = new Date(Date.UTC(year, month - 1, day));
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

export function eachDate(start: string, end: string): string[] {
  const dates: string[] = [];
  for (let cursor = start; cursor <= end; cursor = addDays(cursor, 1)) {
    dates.push(cursor);
  }
  return dates;
}

export function lastDateInclusive(period: AttendancePeriod, today: string): string {
  return period.endDate ?? today;
}

export function availableYears(period: AttendancePeriod, today: string): number[] {
  const lastYear = yearMonthOf(lastDateInclusive(period, today)).year;
  const firstYear = Math.max(yearMonthOf(period.startDate).year, FIRST_HISTORY_YEAR);
  if (firstYear > lastYear) {
    return [yearMonthOf(today).year];
  }
  const years: number[] = [];
  for (let year = lastYear; year >= firstYear; year -= 1) {
    years.push(year);
  }
  return years;
}

export function firstSelectableMonth(period: AttendancePeriod, year: number): YearMonth {
  const january = { year, month: 1 };
  const start = yearMonthOf(period.startDate);
  if (start.year < year || start.year > year) {
    return january;
  }
  return start;
}

export function lastSelectableMonth(
  period: AttendancePeriod,
  year: number,
  today: string,
): YearMonth {
  const now = yearMonthOf(today);
  const december = { year, month: 12 };
  const cap = year === now.year ? now : december;
  const periodCap = yearMonthOf(lastDateInclusive(period, today));
  return [cap, periodCap, december].reduce((min, current) =>
    compareYearMonth(current, min) < 0 ? current : min,
  );
}

export function clampYearMonth(
  year: number,
  month: number,
  period: AttendancePeriod,
  today: string,
): YearMonth {
  const first = firstSelectableMonth(period, year);
  const last = lastSelectableMonth(period, year, today);
  const candidate = { year, month: Math.min(12, Math.max(1, month)) };
  if (compareYearMonth(candidate, first) < 0) {
    return first;
  }
  if (compareYearMonth(candidate, last) > 0) {
    return last;
  }
  return candidate;
}

export function canGoToPreviousMonth(
  selected: YearMonth,
  period: AttendancePeriod,
): boolean {
  return compareYearMonth(selected, firstSelectableMonth(period, selected.year)) > 0;
}

export function canGoToNextMonth(
  selected: YearMonth,
  period: AttendancePeriod,
  today: string,
): boolean {
  return compareYearMonth(selected, lastSelectableMonth(period, selected.year, today)) < 0;
}

export function intersectRange(
  period: AttendancePeriod,
  rangeStart: string,
  rangeEnd: string,
  today: string,
): { start: string; end: string } | null {
  const start = period.startDate > rangeStart ? period.startDate : rangeStart;
  const periodEnd = lastDateInclusive(period, today);
  const end = periodEnd < rangeEnd ? periodEnd : rangeEnd;
  if (start > end) {
    return null;
  }
  return { start, end };
}
