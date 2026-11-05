"use server";

import { revalidatePath } from "next/cache";
import { connection } from "next/server";

import { closePeriod } from "@/application/periods/close-period";
import { closePeriodMessage } from "@/application/periods/close-period-message";
import { attendanceToday } from "@/domain/attendance/attendance-clock";
import { createSupabasePeriodRepository } from "@/infrastructure/attendance/supabase-period-repository";

export type ClosePeriodFormState = {
  errorMessage: string | null;
  successMessage: string | null;
};

export async function closePeriodAction(
  state: ClosePeriodFormState,
  formData: FormData,
): Promise<ClosePeriodFormState> {
  void state;
  await connection();
  const result = await closePeriod(
    createSupabasePeriodRepository(),
    {
      endDate: String(formData.get("endDate") ?? ""),
      newStartDate: String(formData.get("newStartDate") ?? ""),
      newTitle: String(formData.get("newTitle") ?? ""),
    },
    attendanceToday(),
  );

  if (result.type === "success") {
    revalidatePath("/periodos");
    revalidatePath("/");
    revalidatePath("/historial");
    return {
      errorMessage: null,
      successMessage: "Corte hecho. Bloque cerrado y nuevo periodo activo.",
    };
  }

  return { errorMessage: closePeriodMessage(result), successMessage: null };
}
