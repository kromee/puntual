import type { Absence } from "@/domain/attendance/absence";
import type { AttendancePeriod } from "@/domain/attendance/attendance-period";
import type { CheckIn } from "@/domain/attendance/check-in";

export type HistoryRepository = {
  listPeriods(): Promise<AttendancePeriod[] | null>;
  listCheckIns(periodId: number, start: string, end: string): Promise<CheckIn[]>;
  listAbsences(periodId: number, start: string, end: string): Promise<Absence[]>;
};
