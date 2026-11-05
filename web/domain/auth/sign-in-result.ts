import type { AuthSession } from "@/domain/auth/auth-session";

export type SignInResult =
  | { type: "success"; session: AuthSession }
  | { type: "invalid-input" }
  | { type: "invalid-credentials" }
  | { type: "missing-config" }
  | { type: "error"; message: string };
