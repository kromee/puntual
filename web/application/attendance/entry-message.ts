import type { CorrectCheckInResult } from "@/domain/attendance/correct-check-in-result";
import type { RegisterCheckInResult } from "@/domain/attendance/register-check-in-result";

export function registerMessage(result: Exclude<RegisterCheckInResult, { type: "success" }>): string {
  switch (result.type) {
    case "not-workday":
      return "Hoy no es día laborable (lun–vie).";
    case "already-registered":
      return "Ya registraste tu entrada hoy.";
    case "no-active-period":
      return "No hay periodo activo.";
    case "outside-active-period":
      return "Hoy queda fuera del bloque activo.";
    case "missing-function":
      return "Falta aplicar la migración de registro en Supabase.";
    case "network":
      return "No se pudo conectar. Revisa tu internet e intenta de nuevo.";
  }
}

export function correctMessage(result: Exclude<CorrectCheckInResult, { type: "success" }>): string {
  switch (result.type) {
    case "invalid-input":
      return "Indica una hora válida y un motivo de al menos 3 caracteres.";
    case "not-registered":
      return "No hay una entrada de hoy para corregir.";
    case "missing-function":
      return "Falta aplicar la migración de registro en Supabase.";
    case "network":
      return "No se pudo actualizar la hora.";
  }
}
