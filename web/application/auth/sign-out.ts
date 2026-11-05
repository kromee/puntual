import type { AuthRepository } from "@/domain/auth/auth-repository";

export function signOut(repository: AuthRepository): Promise<void> {
  return repository.signOut();
}
