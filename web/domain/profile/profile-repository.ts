import type { UserProfile } from "@/domain/profile/user-profile";
import type { SaveProfileResult } from "@/domain/profile/save-profile";

export type ProfileRepository = {
  getProfile(): Promise<UserProfile>;
  saveProfile(profile: UserProfile): Promise<SaveProfileResult>;
};
