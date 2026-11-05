import { sanitizeDisplayName } from "@/domain/profile/sanitize-display-name";
import type { UserProfile } from "@/domain/profile/user-profile";

export function headerSubtitle(profile: UserProfile, email: string): string {
  const name = sanitizeDisplayName(profile.displayName);
  return name || email;
}
