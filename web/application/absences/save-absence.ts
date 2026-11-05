import { isActiveAbsence, type AbsenceType } from "@/domain/attendance/absence";
import type { AbsenceRepository } from "@/domain/attendance/absence-repository";
import type { AttendancePeriod } from "@/domain/attendance/attendance-period";
import type { SaveAbsenceResult } from "@/domain/attendance/save-absence";

const MAX_REASON_LENGTH = 200;
const ABSENCE_TYPES: readonly AbsenceType[] = [
  "VACATION",
  "PERSONAL_PERMISSION",
  "SICK_LEAVE",
  "HOLIDAY",
  "FIELD_WORK",
];

export async function saveAbsence(
  repository: AbsenceRepository,
  active: AttendancePeriod | null,
  input: { startDate: string; endDate: string; type: string; reason: string },
  today: string,
): Promise<SaveAbsenceResult> {
  const reason = input.reason.trim();
  if (!isIsoDate(input.startDate) || !isIsoDate(input.endDate) || !isKnownAbsenceType(input.type)) {
    return { type: "invalid-input" };
  }
  if (reason.length > MAX_REASON_LENGTH) {
    return { type: "invalid-input" };
  }
  if (input.endDate < input.startDate) {
    return { type: "end-before-start" };
  }
  if (!active) {
    return { type: "no-active-period" };
  }

  const lastDate = active.endDate ?? today;
  const inside = input.startDate >= active.startDate
    && input.endDate <= lastDate
    && input.startDate <= lastDate;
  if (!inside) {
    return { type: "outside-active-period" };
  }

  const overlapping = await repository.listOverlapping(active.id, input.startDate, input.endDate);
  if (overlapping.some(isActiveAbsence)) {
    return { type: "overlaps-existing" };
  }

  return repository.insert({
    periodId: active.id,
    startDate: input.startDate,
    endDate: input.endDate,
    type: input.type,
    reason,
  });
}

function isKnownAbsenceType(value: string): value is AbsenceType {
  return (ABSENCE_TYPES as readonly string[]).includes(value);
}

function isIsoDate(value: string): boolean {
  return /^\d{4}-\d{2}-\d{2}$/.test(value);
}
