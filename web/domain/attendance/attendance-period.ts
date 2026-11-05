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

export function defaultOpenPeriodTitle(): string {
  return "1 ene 2025 – actualidad";
}
