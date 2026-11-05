import type { AuthSession } from "@/domain/auth/auth-session";
import type { SignInResult } from "@/domain/auth/sign-in-result";

export type AuthRepository = {
  signIn(email: string, password: string): Promise<SignInResult>;
  signOut(): Promise<void>;
  getSession(): Promise<AuthSession | null>;
};
