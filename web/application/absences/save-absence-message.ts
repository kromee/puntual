import type { SaveAbsenceResult } from "@/domain/attendance/save-absence";

export function saveAbsenceMessage(result: Exclude<SaveAbsenceResult, { type: "success" }>): string {
  switch (result.type) {
    case "invalid-input":
      return "Revisa las fechas, el tipo y el motivo.";
    case "no-active-period":
      return "No hay un bloque activo para registrar la ausencia.";
    case "end-before-start":
      return "La fecha final no puede ser anterior a la inicial.";
    case "outside-active-period":
      return "La ausencia debe quedar dentro del bloque activo.";
    case "overlaps-existing":
      return "Ya existe una ausencia registrada para una o más fechas de ese rango.";
    case "network":
      return "No se pudo conectar. Revisa tu internet e intenta de nuevo.";
  }
}
