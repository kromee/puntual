import type { RegisterManualCheckInResult } from "@/domain/attendance/register-manual-check-in-result";

export function registerManualCheckInMessage(
  result: Exclude<RegisterManualCheckInResult, { type: "success" }>,
): string {
  switch (result.type) {
    case "invalid-input":
      return "Revisa la fecha y la hora.";
    case "future-date":
      return "No puedes registrar una fecha futura.";
    case "not-workday":
      return "La fecha seleccionada no es día laborable.";
    case "outside-active-period":
      return "La fecha queda fuera del periodo seleccionado.";
    case "already-registered":
      return "Ya existe un registro para esa fecha.";
    case "no-period":
      return "No hay periodo seleccionado para guardar.";
    case "network":
      return "No se pudo conectar. Revisa tu internet e intenta de nuevo.";
  }
}
