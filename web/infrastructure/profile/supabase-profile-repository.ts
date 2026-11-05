import { defaultUserProfile, type UserProfile } from "@/domain/profile/user-profile";
import type { ProfileRepository } from "@/domain/profile/profile-repository";
import type { SaveProfileResult } from "@/domain/profile/save-profile";
import { createSupabaseServerClient } from "@/infrastructure/supabase/server";

type ProfileRow = {
  display_name: string;
  has_expected_time: boolean;
  expected_hour: number | null;
  expected_minute: number | null;
};

export function createSupabaseProfileRepository(): ProfileRepository {
  return {
    getProfile,
    saveProfile,
  };
}

async function getProfile(): Promise<UserProfile> {
  const context = await requireUser();
  if (!context) {
    return defaultUserProfile();
  }
  const { data, error } = await context.supabase
    .from("user_profiles")
    .select("display_name, has_expected_time, expected_hour, expected_minute")
    .eq("user_id", context.userId)
    .maybeSingle();

  if (error) {
    if (isMissingProfileTable(error)) {
      return defaultUserProfile();
    }
    throw error;
  }
  if (!data) {
    return defaultUserProfile();
  }
  return toProfile(data as ProfileRow);
}

async function saveProfile(profile: UserProfile): Promise<SaveProfileResult> {
  const context = await requireUser();
  if (!context) {
    return { type: "network" };
  }
  const row = {
    user_id: context.userId,
    display_name: profile.displayName,
    has_expected_time: profile.hasExpectedTime,
    expected_hour: profile.hasExpectedTime ? profile.expectedHour : null,
    expected_minute: profile.hasExpectedTime ? profile.expectedMinute : null,
    updated_at: new Date().toISOString(),
  };
  const { error } = await context.supabase.from("user_profiles").upsert(row, {
    onConflict: "user_id",
  });

  if (error) {
    if (isMissingProfileTable(error)) {
      return { type: "missing-migration" };
    }
    return { type: "network" };
  }
  return { type: "success" };
}

function isMissingProfileTable(error: { code?: string; message?: string }): boolean {
  if (error.code === "PGRST205") {
    return true;
  }
  const message = error.message ?? "";
  return message.includes("user_profiles");
}

function toProfile(row: ProfileRow): UserProfile {
  const defaults = defaultUserProfile();
  return {
    displayName: row.display_name ?? "",
    hasExpectedTime: row.has_expected_time,
    expectedHour: row.expected_hour ?? defaults.expectedHour,
    expectedMinute: row.expected_minute ?? defaults.expectedMinute,
  };
}

async function requireUser() {
  const supabase = await createSupabaseServerClient();
  const { data } = await supabase.auth.getUser();
  if (!data.user) {
    return null;
  }
  return { supabase, userId: data.user.id };
}
