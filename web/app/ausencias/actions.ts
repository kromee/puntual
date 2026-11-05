"use server";

import { revalidatePath } from "next/cache";
import { connection } from "next/server";

import { saveAbsence } from "@/application/absences/save-absence";
import { saveAbsenceMessage } from "@/application/absences/save-absence-message";
import { attendanceToday } from "@/domain/attendance/attendance-clock";
import { createSupabaseAbsenceRepository } from "@/infrastructure/attendance/supabase-absence-repository";
import { createSupabasePeriodRepository } from "@/infrastructure/attendance/supabase-period-repository";

export type SaveAbsenceFormState = {
  errorMessage: string | null;
  successMessage: string | null;
};

export async function saveAbsenceAction(
  state: SaveAbsenceFormState,
  formData: FormData,
): Promise<SaveAbsenceFormState> {
  void state;
  await connection();
  const today = attendanceToday();
  const active = await createSupabasePeriodRepository().getActivePeriod();
  const result = await saveAbsence(
    createSupabaseAbsenceRepository(),
    active,
    {
      startDate: String(formData.get("startDate") ?? ""),
      endDate: String(formData.get("endDate") ?? ""),
      type: String(formData.get("type") ?? ""),
      reason: String(formData.get("reason") ?? ""),
    },
    today,
  );

  if (result.type === "success") {
    revalidatePath("/ausencias");
    revalidatePath("/historial");
    return { errorMessage: null, successMessage: "Ausencia registrada." };
  }
  return { errorMessage: saveAbsenceMessage(result), successMessage: null };
}

export async function deleteAbsenceAction(formData: FormData): Promise<void> {
  const id = Number(formData.get("id"));
  if (!Number.isInteger(id)) {
    return;
  }
  await createSupabaseAbsenceRepository().delete(id);
  revalidatePath("/ausencias");
  revalidatePath("/historial");
}
