import type { ProfileRepository } from "@/domain/profile/profile-repository";
import { sanitizeDisplayName } from "@/domain/profile/sanitize-display-name";
import type { SaveProfileResult } from "@/domain/profile/save-profile";
import type { UserProfile } from "@/domain/profile/user-profile";

export async function saveProfile(
  repository: ProfileRepository,
  input: {
    displayName: string;
    hasExpectedTime: boolean;
    expectedHour: number;
    expectedMinute: number;
  },
): Promise<SaveProfileResult> {
  const displayName = sanitizeDisplayName(input.displayName);
  if (input.hasExpectedTime) {
    if (
      !Number.isInteger(input.expectedHour)
      || !Number.isInteger(input.expectedMinute)
      || input.expectedHour < 0
      || input.expectedHour > 23
      || input.expectedMinute < 0
      || input.expectedMinute > 59
    ) {
      return { type: "invalid-input" };
    }
  }

  const profile: UserProfile = {
    displayName,
    hasExpectedTime: input.hasExpectedTime,
    expectedHour: input.expectedHour,
    expectedMinute: input.expectedMinute,
  };

  return repository.saveProfile(profile);
}
