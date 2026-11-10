import { connection } from "next/server";

import type { CheckInRepository } from "@/domain/attendance/check-in-repository";
import {
  FIRST_PERIOD_START,
  attendanceToday,
  isWorkday,
} from "@/domain/attendance/attendance-clock";
import {
  defaultOpenPeriodTitle,
  periodContains,
} from "@/domain/attendance/attendance-period";
import type { CheckIn } from "@/domain/attendance/check-in";
import type { CorrectCheckInResult } from "@/domain/attendance/correct-check-in-result";
import type { RegisterCheckInResult } from "@/domain/attendance/register-check-in-result";
import type { RegisterManualCheckInResult } from "@/domain/attendance/register-manual-check-in-result";
import type { TodayEntry } from "@/domain/attendance/today-entry";
import {
  toCheckIn,
  toPeriod,
  type CheckInRow,
  type PeriodRow,
} from "@/infrastructure/attendance/supabase-mapper";
import { createSupabaseServerClient } from "@/infrastructure/supabase/server";

type PostgrestError = {
  code?: string;
  message?: string;
};

export function createSupabaseCheckInRepository(): CheckInRepository {
  return {
    getTodayEntry,
    registerCheckIn,
    correctCheckIn,
    findCheckIn,
    insertManualCheckIn,
  };
}

async function getTodayEntry(): Promise<TodayEntry | null> {
  await connection();
  const supabase = await createSupabaseServerClient();
  const { data } = await supabase.auth.getUser();
  const userId = data.user?.id;
  if (!userId) {
    return null;
  }

  const workDate = attendanceToday();
  const period = await activePeriod(supabase, userId);
  const { data: checkInRow, error } = await supabase
    .from("check_ins")
    .select("id, period_id, work_date, checked_in_at, expected_hour, expected_minute, delay_minutes")
    .eq("user_id", userId)
    .eq("period_id", period.id)
    .eq("work_date", workDate)
    .limit(1)
    .maybeSingle();

  if (error) {
    throw error;
  }

  return {
    workDate,
    isWorkday: isWorkday(workDate),
    period,
    checkIn: checkInRow ? toCheckIn(checkInRow as CheckInRow) : null,
    outsidePeriod: !periodContains(period, workDate),
  };
}

async function registerCheckIn(): Promise<RegisterCheckInResult> {
  const supabase = await createSupabaseServerClient();
  const { data, error } = await supabase.rpc("register_check_in");
  if (error || !data) {
    return error ? registerFailure(error) : { type: "network" };
  }
  return { type: "success", checkIn: toCheckIn(oneRow(data)) };
}

async function correctCheckIn(input: {
  hour: number;
  minute: number;
  reason: string;
}): Promise<CorrectCheckInResult> {
  const supabase = await createSupabaseServerClient();
  const { data, error } = await supabase.rpc("correct_check_in", {
    p_hour: input.hour,
    p_minute: input.minute,
    p_reason: input.reason,
  });
  if (error || !data) {
    return error ? correctFailure(error) : { type: "network" };
  }
  return { type: "success", checkIn: toCheckIn(oneRow(data)) };
}

async function findCheckIn(periodId: number, workDate: string): Promise<CheckIn | null> {
  await connection();
  const context = await requireUser();
  if (!context) {
    return null;
  }
  const { data, error } = await context.supabase
    .from("check_ins")
    .select("id, period_id, work_date, checked_in_at, expected_hour, expected_minute, delay_minutes")
    .eq("user_id", context.userId)
    .eq("period_id", periodId)
    .eq("work_date", workDate)
    .limit(1)
    .maybeSingle();

  if (error) {
    throw error;
  }
  return data ? toCheckIn(data as CheckInRow) : null;
}

async function insertManualCheckIn(input: {
  periodId: number;
  workDate: string;
  checkedInAt: string;
  expectedHour: number;
  expectedMinute: number;
  delayMinutes: number;
}): Promise<RegisterManualCheckInResult> {
  const context = await requireUser();
  if (!context) {
    return { type: "network" };
  }
  const { error } = await context.supabase.from("check_ins").insert({
    user_id: context.userId,
    period_id: input.periodId,
    work_date: input.workDate,
    checked_in_at: input.checkedInAt,
    expected_hour: input.expectedHour,
    expected_minute: input.expectedMinute,
    delay_minutes: input.delayMinutes,
  });

  if (error) {
    if (error.code === "23505") {
      return { type: "already-registered" };
    }
    return { type: "network" };
  }
  return { type: "success" };
}

async function activePeriod(
  supabase: Awaited<ReturnType<typeof createSupabaseServerClient>>,
  userId: string,
): Promise<TodayEntry["period"]> {
  const { data, error } = await supabase
    .from("attendance_periods")
    .select("id, title, start_date, end_date, is_active")
    .eq("user_id", userId)
    .eq("is_active", true)
    .order("id", { ascending: false })
    .limit(1)
    .maybeSingle();

  if (error) {
    throw error;
  }
  if (data) {
    return toPeriod(data as PeriodRow);
  }

  const { data: created, error: insertError } = await supabase
    .from("attendance_periods")
    .insert({
      user_id: userId,
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
  return toPeriod(created as PeriodRow);
}

function registerFailure(error: PostgrestError): RegisterCheckInResult {
  const message = error.message ?? "";
  if (isMissingFunction(error, message)) {
    return { type: "missing-function" };
  }
  if (message.includes("not_workday")) {
    return { type: "not-workday" };
  }
  if (message.includes("already_registered") || error.code === "23505") {
    return { type: "already-registered" };
  }
  if (message.includes("no_active_period")) {
    return { type: "no-active-period" };
  }
  if (message.includes("outside_active_period")) {
    return { type: "outside-active-period" };
  }
  return { type: "network" };
}

function correctFailure(error: PostgrestError): CorrectCheckInResult {
  const message = error.message ?? "";
  if (isMissingFunction(error, message)) {
    return { type: "missing-function" };
  }
  if (message.includes("invalid_input")) {
    return { type: "invalid-input" };
  }
  if (message.includes("not_registered")) {
    return { type: "not-registered" };
  }
  return { type: "network" };
}

function oneRow(data: CheckInRow | CheckInRow[]): CheckInRow {
  return Array.isArray(data) ? data[0] : data;
}

function isMissingFunction(error: PostgrestError, message: string): boolean {
  return error.code === "PGRST202" || message.includes("Could not find the function");
}

async function requireUser() {
  const supabase = await createSupabaseServerClient();
  const { data } = await supabase.auth.getUser();
  if (!data.user) {
    return null;
  }
  return { supabase, userId: data.user.id };
}
