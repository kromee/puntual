import type { AbsenceRepository } from "@/domain/attendance/absence-repository";
import type { SaveAbsenceResult } from "@/domain/attendance/save-absence";
import { toAbsence, type AbsenceRow } from "@/infrastructure/attendance/supabase-mapper";
import { createSupabaseServerClient } from "@/infrastructure/supabase/server";

export function createSupabaseAbsenceRepository(): AbsenceRepository {
  return {
    listForPeriod,
    listOverlapping,
    insert,
    delete: deleteAbsence,
  };
}

async function listForPeriod(periodId: number) {
  const context = await requireUser();
  if (!context) {
    return [];
  }
  const { data, error } = await context.supabase
    .from("absences")
    .select("id, period_id, start_date, end_date, type, reason, status")
    .eq("user_id", context.userId)
    .eq("period_id", periodId)
    .order("start_date", { ascending: false });

  if (error) {
    throw error;
  }
  return ((data ?? []) as AbsenceRow[]).map(toAbsence);
}

async function listOverlapping(periodId: number, startDate: string, endDate: string) {
  const context = await requireUser();
  if (!context) {
    return [];
  }
  const { data, error } = await context.supabase
    .from("absences")
    .select("id, period_id, start_date, end_date, type, reason, status")
    .eq("user_id", context.userId)
    .eq("period_id", periodId)
    .lte("start_date", endDate)
    .gte("end_date", startDate);

  if (error) {
    throw error;
  }
  return ((data ?? []) as AbsenceRow[]).map(toAbsence);
}

async function insert(input: {
  periodId: number;
  startDate: string;
  endDate: string;
  type: "VACATION" | "PERSONAL_PERMISSION" | "SICK_LEAVE" | "HOLIDAY" | "FIELD_WORK";
  reason: string;
}): Promise<SaveAbsenceResult> {
  const context = await requireUser();
  if (!context) {
    return { type: "no-active-period" };
  }
  const { error } = await context.supabase.from("absences").insert({
    user_id: context.userId,
    period_id: input.periodId,
    start_date: input.startDate,
    end_date: input.endDate,
    type: input.type,
    reason: input.reason,
    status: "APPROVED",
  });

  if (error) {
    return { type: "network" };
  }
  return { type: "success" };
}

async function deleteAbsence(absenceId: number) {
  const context = await requireUser();
  if (!context) {
    return;
  }
  const { error } = await context.supabase
    .from("absences")
    .delete()
    .eq("id", absenceId)
    .eq("user_id", context.userId);

  if (error) {
    throw error;
  }
}

async function requireUser() {
  const supabase = await createSupabaseServerClient();
  const { data } = await supabase.auth.getUser();
  if (!data.user) {
    return null;
  }
  return { supabase, userId: data.user.id };
}
