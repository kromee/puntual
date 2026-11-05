import type { Absence, AbsenceType } from "@/domain/attendance/absence";
import type { SaveAbsenceResult } from "@/domain/attendance/save-absence";

export type AbsenceRepository = {
  listForPeriod(periodId: number): Promise<Absence[]>;
  listOverlapping(periodId: number, startDate: string, endDate: string): Promise<Absence[]>;
  insert(input: {
    periodId: number;
    startDate: string;
    endDate: string;
    type: AbsenceType;
    reason: string;
  }): Promise<SaveAbsenceResult>;
  delete(absenceId: number): Promise<void>;
};
