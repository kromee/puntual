import { Suspense, type ReactNode } from "react";

import { getAuthenticatedShell } from "@/application/shell/get-authenticated-shell";
import { ProfileForm } from "@/components/profile/profile-form";
import { AppHeader } from "@/components/shell/app-header";
import { createSupabaseAuthRepository } from "@/infrastructure/auth/supabase-auth-repository";
import { createSupabaseProfileRepository } from "@/infrastructure/profile/supabase-profile-repository";

export default function ProfilePage() {
  return (
    <Suspense fallback={<ProfileShell subtitle="Comprobando tu acceso." />}>
      <ProfileContent />
    </Suspense>
  );
}

async function ProfileContent() {
  const { profile, subtitle } = await getAuthenticatedShell(
    createSupabaseAuthRepository(),
    createSupabaseProfileRepository(),
  );

  return (
    <ProfileShell subtitle={subtitle}>
      <h1 className="text-xl font-semibold">Perfil</h1>
      <p className="mt-2 text-sm text-puntual-secondary">
        Nombre visible y hora esperada se guardan en Supabase para la web. Si aún no
        ejecutaste el SQL, usa{" "}
        <span className="font-medium">supabase/migrations/20261105121500_user_profiles.sql</span>.
      </p>
      <ProfileForm
        displayName={profile.displayName}
        hasExpectedTime={profile.hasExpectedTime}
        expectedHour={profile.expectedHour}
        expectedMinute={profile.expectedMinute}
      />
    </ProfileShell>
  );
}

function ProfileShell({ subtitle, children }: { subtitle: string; children?: ReactNode }) {
  return (
    <main className="mx-auto flex min-h-full w-full max-w-md flex-col px-4 py-8">
      <AppHeader subtitle={subtitle} current="perfil" />
      <section className="mt-6 rounded-3xl bg-white px-6 py-8">{children}</section>
    </main>
  );
}
