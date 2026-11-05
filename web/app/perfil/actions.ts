"use server";

import { revalidatePath } from "next/cache";

import { saveProfile } from "@/application/profile/save-profile";
import { saveProfileMessage } from "@/application/profile/save-profile-message";
import { parseClockInput } from "@/application/profile/format-expected-time";
import { DEFAULT_EXPECTED_HOUR, DEFAULT_EXPECTED_MINUTE } from "@/domain/profile/user-profile";
import { createSupabaseProfileRepository } from "@/infrastructure/profile/supabase-profile-repository";

export type SaveProfileFormState = {
  errorMessage: string | null;
  successMessage: string | null;
};

export async function saveProfileAction(
  state: SaveProfileFormState,
  formData: FormData,
): Promise<SaveProfileFormState> {
  void state;
  const useExpectedTime = formData.get("useExpectedTime") === "on";
  let expectedHour = DEFAULT_EXPECTED_HOUR;
  let expectedMinute = DEFAULT_EXPECTED_MINUTE;

  if (useExpectedTime) {
    const parsed = parseClockInput(String(formData.get("expectedTime") ?? ""));
    if (!parsed) {
      return { errorMessage: "Revisa la hora esperada.", successMessage: null };
    }
    expectedHour = parsed.hour;
    expectedMinute = parsed.minute;
  }

  const result = await saveProfile(createSupabaseProfileRepository(), {
    displayName: String(formData.get("displayName") ?? ""),
    hasExpectedTime: useExpectedTime,
    expectedHour,
    expectedMinute,
  });

  if (result.type === "success") {
    revalidatePath("/", "layout");
    revalidatePath("/perfil");
    return { errorMessage: null, successMessage: "Perfil guardado." };
  }
  return { errorMessage: saveProfileMessage(result), successMessage: null };
}
