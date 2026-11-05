export type Absence = {
  id: number;
  periodId: number;
  startDate: string;
  endDate: string;
  type: string;
  reason: string;
  status: string;
};

const ABSENCE_LABELS: Record<string, string> = {
  VACATION: "Vacaciones",
  PERSONAL_PERMISSION: "Permiso",
  SICK_LEAVE: "Incapacidad",
  HOLIDAY: "Día festivo",
  FIELD_WORK: "Trabajo fuera de oficina",
};

const STATUS_LABELS: Record<string, string> = {
  PENDING: "Pendiente",
  APPROVED: "Aprobado",
  REJECTED: "Rechazado",
  CANCELLED: "Cancelado",
};

export function isActiveAbsence(absence: Absence): boolean {
  return absence.status !== "REJECTED" && absence.status !== "CANCELLED";
}

export function absenceTypeLabel(type: string): string {
  return ABSENCE_LABELS[type] ?? type;
}

export function absenceStatusLabel(status: string): string {
  return STATUS_LABELS[status] ?? status;
}
