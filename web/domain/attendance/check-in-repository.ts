import type { CheckIn } from "@/domain/attendance/check-in";
import type { CorrectCheckInResult } from "@/domain/attendance/correct-check-in-result";
import type { RegisterCheckInResult } from "@/domain/attendance/register-check-in-result";
import type { RegisterManualCheckInResult } from "@/domain/attendance/register-manual-check-in-result";
import type { TodayEntry } from "@/domain/attendance/today-entry";

export type CheckInRepository = {
  getTodayEntry(): Promise<TodayEntry | null>;
  registerCheckIn(): Promise<RegisterCheckInResult>;
  correctCheckIn(input: {
    hour: number;
    minute: number;
    reason: string;
  }): Promise<CorrectCheckInResult>;
  findCheckIn(periodId: number, workDate: string): Promise<CheckIn | null>;
  insertManualCheckIn(input: {
    periodId: number;
    workDate: string;
    checkedInAt: string;
    expectedHour: number;
    expectedMinute: number;
    delayMinutes: number;
  }): Promise<RegisterManualCheckInResult>;
};
