import { mexicoCheckInTimestamp } from "@/application/attendance/mexico-check-in-timestamp";
import type { CheckInRepository } from "@/domain/attendance/check-in-repository";
import { isWorkday } from "@/domain/attendance/attendance-clock";
import type { AttendancePeriod } from "@/domain/attendance/attendance-period";
import { periodContainsWorkDate } from "@/domain/attendance/attendance-period";
import { delayMinutesSameDay } from "@/domain/attendance/delay-calculator";
import type { RegisterManualCheckInResult } from "@/domain/attendance/register-manual-check-in-result";
import type { UserProfile } from "@/domain/profile/user-profile";

export async function registerManualCheckIn(
  repository: CheckInRepository,
  period: AttendancePeriod | null,
  profile: UserProfile,
  today: string,
  input: { workDate: string; hour: number; minute: number },
): Promise<RegisterManualCheckInResult> {
  if (
    !/^\d{4}-\d{2}-\d{2}$/.test(input.workDate)
    || !Number.isInteger(input.hour)
    || !Number.isInteger(input.minute)
    || input.hour < 0
    || input.hour > 23
    || input.minute < 0
    || input.minute > 59
  ) {
    return { type: "invalid-input" };
  }
  if (input.workDate > today) {
    return { type: "future-date" };
  }
  if (!isWorkday(input.workDate)) {
    return { type: "not-workday" };
  }
  if (!period) {
    return { type: "no-period" };
  }
  if (!periodContainsWorkDate(period, input.workDate, today)) {
    return { type: "outside-active-period" };
  }

  const existing = await repository.findCheckIn(period.id, input.workDate);
  if (existing) {
    return { type: "already-registered" };
  }

  const expectedHour = profile.hasExpectedTime ? profile.expectedHour : input.hour;
  const expectedMinute = profile.hasExpectedTime ? profile.expectedMinute : input.minute;
  const delayMinutes = profile.hasExpectedTime
    ? delayMinutesSameDay(input.hour, input.minute, expectedHour, expectedMinute)
    : 0;

  return repository.insertManualCheckIn({
    periodId: period.id,
    workDate: input.workDate,
    checkedInAt: mexicoCheckInTimestamp(input.workDate, input.hour, input.minute),
    expectedHour,
    expectedMinute,
    delayMinutes,
  });
}
