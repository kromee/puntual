import type { AttendancePeriod } from "@/domain/attendance/attendance-period";
import type { CheckIn } from "@/domain/attendance/check-in";

export type TodayEntry = {
  workDate: string;
  isWorkday: boolean;
  period: AttendancePeriod;
  checkIn: CheckIn | null;
  outsidePeriod: boolean;
};
