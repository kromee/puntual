import { FIRST_PERIOD_START } from "@/domain/attendance/attendance-clock";
import { defaultOpenPeriodTitle } from "@/domain/attendance/attendance-period";
import type { HistoryRepository } from "@/domain/attendance/history-repository";
import {
  toAbsence,
  toCheckIn,
  toPeriod,
  type AbsenceRow,
  type CheckInRow,
  type PeriodRow,
} from "@/infrastructure/attendance/supabase-mapper";
import { createSupabaseServerClient } from "@/infrastructure/supabase/server";

export function createSupabaseHistoryRepository(): HistoryRepository {
  return {
    listPeriods,
    listCheckIns,
    listAbsences,
  };
}

async function listPeriods() {
  const context = await requireUser();
  if (!context) {
    return null;
  }

  const { data, error } = await context.supabase
    .from("attendance_periods")
    .select("id, title, start_date, end_date, is_active")
    .eq("user_id", context.userId)
    .order("start_date", { ascending: false })
    .order("id", { ascending: false });

  if (error) {
    throw error;
  }

  const periods = ((data ?? []) as PeriodRow[]).map(toPeriod);
  if (periods.length > 0) {
    return periods;
  }

  const { data: created, error: insertError } = await context.supabase
    .from("attendance_periods")
    .insert({
      user_id: context.userId,
      title: defaultOpenPeriodTitle(),
      start_date: FIRST_PERIOD_START,
      end_date: null,
      is_active: true,
    })
    .select("id, title, start_date, end_date, is_active")
    .single();

  if (insertError) {
    throw insertError;
  }
  return [toPeriod(created as PeriodRow)];
}

async function listCheckIns(periodId: number, start: string, end: string) {
  const context = await requireUser();
  if (!context) {
    return [];
  }
  const { data, error } = await context.supabase
    .from("check_ins")
    .select("id, period_id, work_date, checked_in_at, expected_hour, expected_minute, delay_minutes")
    .eq("user_id", context.userId)
    .eq("period_id", periodId)
    .gte("work_date", start)
    .lte("work_date", end)
    .order("work_date", { ascending: true });

  if (error) {
    throw error;
  }
  return ((data ?? []) as CheckInRow[]).map(toCheckIn);
}

async function listAbsences(periodId: number, start: string, end: string) {
  const context = await requireUser();
  if (!context) {
    return [];
  }
  const { data, error } = await context.supabase
    .from("absences")
    .select("id, period_id, start_date, end_date, type, reason, status")
    .eq("user_id", context.userId)
    .eq("period_id", periodId)
    .lte("start_date", end)
    .gte("end_date", start)
    .order("start_date", { ascending: true });

  if (error) {
    throw error;
  }
  return ((data ?? []) as AbsenceRow[]).map(toAbsence);
}

async function requireUser() {
  const supabase = await createSupabaseServerClient();
  const { data } = await supabase.auth.getUser();
  if (!data.user) {
    return null;
  }
  return { supabase, userId: data.user.id };
}
