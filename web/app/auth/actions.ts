"use server";

import { redirect } from "next/navigation";

import { signIn } from "@/application/auth/sign-in";
import { signInMessage } from "@/application/auth/sign-in-message";
import { signOut } from "@/application/auth/sign-out";
import { createSupabaseAuthRepository } from "@/infrastructure/auth/supabase-auth-repository";

export type SignInFormState = {
  errorMessage: string | null;
};

export async function signInAction(
  _state: SignInFormState,
  formData: FormData,
): Promise<SignInFormState> {
  const result = await signIn(createSupabaseAuthRepository(), {
    email: String(formData.get("email") ?? ""),
    password: String(formData.get("password") ?? ""),
  });

  if (result.type === "success") {
    redirect("/");
  }

  return { errorMessage: signInMessage(result) };
}

export async function signOutAction(): Promise<void> {
  await signOut(createSupabaseAuthRepository());
  redirect("/login");
}
