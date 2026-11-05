import { Suspense } from "react";

import { getAuthenticatedShell } from "@/application/shell/get-authenticated-shell";
import { ProfileForm } from "@/components/profile/profile-form";
import { AppShell } from "@/components/shell/app-shell";
import { createSupabaseAuthRepository } from "@/infrastructure/auth/supabase-auth-repository";
import { createSupabaseProfileRepository } from "@/infrastructure/profile/supabase-profile-repository";

export default function ProfilePage() {
  return (
    <Suspense fallback={<AppShell subtitle="Comprobando tu acceso." current="perfil" />}>
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
    <AppShell subtitle={subtitle} current="perfil">
      <h1 className="text-xl font-semibold">Perfil</h1>
      <p className="mt-2 text-sm text-puntual-secondary">
        Nombre visible y hora esperada se guardan en Supabase para la web.
      </p>
      <ProfileForm
        displayName={profile.displayName}
        hasExpectedTime={profile.hasExpectedTime}
        expectedHour={profile.expectedHour}
        expectedMinute={profile.expectedMinute}
      />
    </AppShell>
  );
}
