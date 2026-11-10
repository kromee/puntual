export type AttendancePeriod = {
  id: number;
  title: string;
  startDate: string;
  endDate: string | null;
  isActive: boolean;
};

export function periodContains(period: AttendancePeriod, isoDate: string): boolean {
  if (isoDate < period.startDate) {
    return false;
  }
  if (period.endDate && isoDate > period.endDate) {
    return false;
  }
  return true;
}

export function periodContainsWorkDate(
  period: AttendancePeriod,
  isoDate: string,
  today: string,
): boolean {
  if (isoDate < period.startDate) {
    return false;
  }
  const lastDate = period.endDate ?? today;
  return isoDate <= lastDate;
}

export function defaultOpenPeriodTitle(): string {
  return "1 ene 2025 – actualidad";
}
