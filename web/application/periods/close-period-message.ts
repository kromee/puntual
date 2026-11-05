import type { ClosePeriodResult } from "@/domain/attendance/close-period";

export function closePeriodMessage(result: Exclude<ClosePeriodResult, { type: "success" }>): string {
  switch (result.type) {
    case "invalid-input":
      return "Indica el último día del bloque y el primer día del nuevo.";
    case "no-active-period":
      return "No hay un periodo activo para cerrar.";
    case "end-before-start":
      return "La fecha de cierre no puede ser anterior al inicio del bloque.";
    case "new-start-not-after-end":
      return "El nuevo bloque debe empezar después del cierre.";
    case "new-start-in-future":
      return "El inicio del nuevo bloque no puede ser después de hoy.";
    case "network":
      return "No se pudo conectar. Revisa tu internet e intenta de nuevo.";
  }
}
