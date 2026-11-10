"use server";

import { revalidatePath } from "next/cache";
import { connection } from "next/server";

import { registerManualCheckIn } from "@/application/attendance/register-manual-check-in";
import { registerManualCheckInMessage } from "@/application/attendance/register-manual-check-in-message";
import { attendanceToday } from "@/domain/attendance/attendance-clock";
import { createSupabaseCheckInRepository } from "@/infrastructure/attendance/supabase-check-in-repository";
import { createSupabaseHistoryRepository } from "@/infrastructure/attendance/supabase-history-repository";
import { createSupabaseProfileRepository } from "@/infrastructure/profile/supabase-profile-repository";

export type ManualCheckInFormState = {
  errorMessage: string | null;
  successMessage: string | null;
};

export async function registerManualCheckInAction(
  state: ManualCheckInFormState,
  formData: FormData,
): Promise<ManualCheckInFormState> {
  void state;
  await connection();
  const today = attendanceToday();
  const periodId = Number(formData.get("periodId"));
  const workDate = String(formData.get("workDate") ?? "");
  const time = String(formData.get("time") ?? "");
  const clock = parseClock(time);

  const periods = await createSupabaseHistoryRepository().listPeriods();
  const period = periods?.find((item) => item.id === periodId) ?? null;
  const profile = await createSupabaseProfileRepository().getProfile();

  const result = await registerManualCheckIn(
    createSupabaseCheckInRepository(),
    period,
    profile,
    today,
    clock ? { workDate, hour: clock.hour, minute: clock.minute } : { workDate, hour: -1, minute: -1 },
  );

  if (result.type === "success") {
    revalidatePath("/historial");
    return { errorMessage: null, successMessage: "Registro agregado correctamente." };
  }
  return {
    errorMessage: registerManualCheckInMessage(result),
    successMessage: null,
  };
}

function parseClock(value: string): { hour: number; minute: number } | null {
  const match = /^(\d{2}):(\d{2})$/.exec(value);
  if (!match) {
    return null;
  }
  const hour = Number(match[1]);
  const minute = Number(match[2]);
  if (hour > 23 || minute > 59) {
    return null;
  }
  return { hour, minute };
}
