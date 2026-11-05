import { connection } from "next/server";
import { redirect } from "next/navigation";

import { getSession } from "@/application/auth/get-session";
import { headerSubtitle } from "@/application/shell/header-subtitle";
import type { AuthRepository } from "@/domain/auth/auth-repository";
import type { ProfileRepository } from "@/domain/profile/profile-repository";

export async function getAuthenticatedShell(
  authRepository: AuthRepository,
  profileRepository: ProfileRepository,
) {
  await connection();
  const session = await getSession(authRepository);
  if (!session) {
    redirect("/login");
  }
  const profile = await profileRepository.getProfile();
  return {
    session,
    profile,
    subtitle: headerSubtitle(profile, session.email),
  };
}
