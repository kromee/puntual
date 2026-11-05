import type { SaveProfileResult } from "@/domain/profile/save-profile";

export function saveProfileMessage(result: Exclude<SaveProfileResult, { type: "success" }>): string {
  switch (result.type) {
    case "invalid-input":
      return "Revisa la hora esperada.";
    case "missing-migration":
      return "Falta aplicar en Supabase la migración user_profiles (20261105121500_user_profiles.sql).";
    case "network":
      return "No se pudo guardar. Revisa tu internet e intenta de nuevo.";
  }
}
