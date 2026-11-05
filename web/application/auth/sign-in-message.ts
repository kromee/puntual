import type { SignInResult } from "@/domain/auth/sign-in-result";

export function signInMessage(result: Exclude<SignInResult, { type: "success" }>): string {
  switch (result.type) {
    case "invalid-input":
      return "Ingresa correo y contraseña.";
    case "invalid-credentials":
      return "Correo o contraseña incorrectos.";
    case "missing-config":
      return "Falta configurar Supabase.";
    case "error":
      return result.message || "No se pudo iniciar sesión.";
  }
}
