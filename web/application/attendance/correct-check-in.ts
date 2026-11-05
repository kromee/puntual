import type { CheckInRepository } from "@/domain/attendance/check-in-repository";
import type { CorrectCheckInResult } from "@/domain/attendance/correct-check-in-result";

const MIN_REASON_LENGTH = 3;
const MAX_REASON_LENGTH = 200;

export function correctCheckIn(
  repository: CheckInRepository,
  input: { time: string; reason: string },
): Promise<CorrectCheckInResult> {
  const clock = parseClock(input.time);
  const reason = input.reason.trim();
  if (!clock || reason.length < MIN_REASON_LENGTH || reason.length > MAX_REASON_LENGTH) {
    return Promise.resolve({ type: "invalid-input" });
  }

  return repository.correctCheckIn({ ...clock, reason });
}

function parseClock(value: string): { hour: number; minute: number } | null {
  const match = /^(\d{2}):(\d{2})$/.exec(value);
  if (!match) {
    return null;
  }
  const hour = Number(match[1]);
  const minute = Number(match[2]);
  if (hour > 23 || minute > 59) {
    return null;
  }
  return { hour, minute };
}
