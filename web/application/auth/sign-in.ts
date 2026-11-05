import type { AuthRepository } from "@/domain/auth/auth-repository";
import type { SignInResult } from "@/domain/auth/sign-in-result";

type SignInInput = {
  email: string;
  password: string;
};

export async function signIn(
  repository: AuthRepository,
  input: SignInInput,
): Promise<SignInResult> {
  const email = input.email.trim();
  if (!email || !input.password) {
    return { type: "invalid-input" };
  }

  return repository.signIn(email, input.password);
}
