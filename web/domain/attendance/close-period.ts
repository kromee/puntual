import { addDays } from "@/domain/attendance/period-date-rules";

export type ClosePeriodResult =
  | { type: "success" }
  | { type: "invalid-input" }
  | { type: "no-active-period" }
  | { type: "end-before-start" }
  | { type: "new-start-not-after-end" }
  | { type: "new-start-in-future" }
  | { type: "network" };

export function suggestCloseDates(periodStart: string, today: string): { endDate: string; newStartDate: string } {
  let endDate = addDays(today, -1);
  if (endDate < periodStart) {
    endDate = periodStart;
  }
  let newStartDate = addDays(endDate, 1);
  if (newStartDate > today) {
    newStartDate = today;
  }
  if (!(newStartDate > endDate)) {
    newStartDate = today;
  }
  return { endDate, newStartDate };
}

export function validateClosePeriod(
  periodStart: string,
  endDate: string,
  newStartDate: string,
  today: string,
): Exclude<ClosePeriodResult, { type: "success" }> | null {
  if (!isIsoDate(endDate) || !isIsoDate(newStartDate)) {
    return { type: "invalid-input" };
  }
  if (endDate < periodStart) {
    return { type: "end-before-start" };
  }
  if (!(newStartDate > endDate)) {
    return { type: "new-start-not-after-end" };
  }
  if (newStartDate > today) {
    return { type: "new-start-in-future" };
  }
  return null;
}

function isIsoDate(value: string): boolean {
  return /^\d{4}-\d{2}-\d{2}$/.test(value);
}
