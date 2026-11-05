import type { AttendancePeriod } from "@/domain/attendance/attendance-period";
import type { CheckIn } from "@/domain/attendance/check-in";

export type PeriodRow = {
  id: number;
  title: string;
  start_date: string;
  end_date: string | null;
  is_active: boolean;
};

export type CheckInRow = {
  id: number;
  period_id: number;
  work_date: string;
  checked_in_at: string;
  expected_hour: number;
  expected_minute: number;
  delay_minutes: number;
};

export function toPeriod(row: PeriodRow): AttendancePeriod {
  return {
    id: row.id,
    title: row.title,
    startDate: asDate(row.start_date),
    endDate: row.end_date ? asDate(row.end_date) : null,
    isActive: row.is_active,
  };
}

export function toCheckIn(row: CheckInRow): CheckIn {
  return {
    id: row.id,
    periodId: row.period_id,
    workDate: asDate(row.work_date),
    checkedInAt: row.checked_in_at,
    expectedHour: row.expected_hour,
    expectedMinute: row.expected_minute,
    delayMinutes: row.delay_minutes,
  };
}

function asDate(value: string): string {
  return value.slice(0, 10);
}
