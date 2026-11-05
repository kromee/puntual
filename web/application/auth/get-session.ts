import type { AuthRepository } from "@/domain/auth/auth-repository";
import type { AuthSession } from "@/domain/auth/auth-session";

export function getSession(repository: AuthRepository): Promise<AuthSession | null> {
  return repository.getSession();
}
