import type { ClosePeriodResult } from "@/domain/attendance/close-period";
import type { PeriodRepository } from "@/domain/attendance/period-repository";
import { toPeriod, type PeriodRow } from "@/infrastructure/attendance/supabase-mapper";
import { createSupabaseServerClient } from "@/infrastructure/supabase/server";

export function createSupabasePeriodRepository(): PeriodRepository {
  return {
    getActivePeriod,
    closeActivePeriod,
  };
}

async function getActivePeriod() {
  const context = await requireUser();
  if (!context) {
    return null;
  }
  const { data, error } = await context.supabase
    .from("attendance_periods")
    .select("id, title, start_date, end_date, is_active")
    .eq("user_id", context.userId)
    .eq("is_active", true)
    .order("id", { ascending: false })
    .limit(1)
    .maybeSingle();

  if (error) {
    throw error;
  }
  return data ? toPeriod(data as PeriodRow) : null;
}

async function closeActivePeriod(input: {
  periodId: number;
  closedTitle: string;
  endDate: string;
  openedTitle: string;
  newStartDate: string;
}): Promise<ClosePeriodResult> {
  const context = await requireUser();
  if (!context) {
    return { type: "no-active-period" };
  }

  const { data: closed, error: updateError } = await context.supabase
    .from("attendance_periods")
    .update({
      title: input.closedTitle,
      end_date: input.endDate,
      is_active: false,
    })
    .eq("id", input.periodId)
    .eq("user_id", context.userId)
    .eq("is_active", true)
    .select("id");

  if (updateError) {
    return { type: "network" };
  }
  if (!closed || closed.length === 0) {
    return { type: "no-active-period" };
  }

  const { error: insertError } = await context.supabase
    .from("attendance_periods")
    .insert({
      user_id: context.userId,
      title: input.openedTitle,
      start_date: input.newStartDate,
      end_date: null,
      is_active: true,
    });

  if (insertError) {
    return { type: "network" };
  }
  return { type: "success" };
}

async function requireUser() {
  const supabase = await createSupabaseServerClient();
  const { data } = await supabase.auth.getUser();
  if (!data.user) {
    return null;
  }
  return { supabase, userId: data.user.id };
}
