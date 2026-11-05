"use server";

import { revalidatePath } from "next/cache";

import { correctCheckIn } from "@/application/attendance/correct-check-in";
import { correctMessage, registerMessage } from "@/application/attendance/entry-message";
import { registerCheckIn } from "@/application/attendance/register-check-in";
import { createSupabaseCheckInRepository } from "@/infrastructure/attendance/supabase-check-in-repository";

export type EntryFormState = {
  errorMessage: string | null;
};

export async function registerCheckInAction(
  state: EntryFormState,
  formData: FormData,
): Promise<EntryFormState> {
  void state;
  void formData;
  const result = await registerCheckIn(createSupabaseCheckInRepository());
  if (result.type === "success") {
    revalidatePath("/");
    return { errorMessage: null };
  }
  return { errorMessage: registerMessage(result) };
}

export async function correctCheckInAction(
  state: EntryFormState,
  formData: FormData,
): Promise<EntryFormState> {
  void state;
  const result = await correctCheckIn(createSupabaseCheckInRepository(), {
    time: String(formData.get("time") ?? ""),
    reason: String(formData.get("reason") ?? ""),
  });
  if (result.type === "success") {
    revalidatePath("/");
    return { errorMessage: null };
  }
  return { errorMessage: correctMessage(result) };
}
