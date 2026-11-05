import type { AttendancePeriod } from "@/domain/attendance/attendance-period";
import type { ClosePeriodResult } from "@/domain/attendance/close-period";

export type PeriodRepository = {
  getActivePeriod(): Promise<AttendancePeriod | null>;
  closeActivePeriod(input: {
    periodId: number;
    closedTitle: string;
    endDate: string;
    openedTitle: string;
    newStartDate: string;
  }): Promise<ClosePeriodResult>;
};
