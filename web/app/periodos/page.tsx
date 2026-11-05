import { redirect } from "next/navigation";
import { Suspense, type ReactNode } from "react";

import { getAuthenticatedShell } from "@/application/shell/get-authenticated-shell";
import { formatWorkDate } from "@/application/attendance/format-entry";
import { attendanceToday } from "@/domain/attendance/attendance-clock";
import { suggestCloseDates } from "@/domain/attendance/close-period";
import { ClosePeriodForm } from "@/components/periods/close-period-form";
import { AppHeader } from "@/components/shell/app-header";
import { createSupabaseAuthRepository } from "@/infrastructure/auth/supabase-auth-repository";
import { createSupabaseHistoryRepository } from "@/infrastructure/attendance/supabase-history-repository";
import { createSupabaseProfileRepository } from "@/infrastructure/profile/supabase-profile-repository";

export default function PeriodsPage() {
  return (
    <Suspense fallback={<PeriodsShell subtitle="Comprobando tu acceso." />}>
      <PeriodsContent />
    </Suspense>
  );
}

async function PeriodsContent() {
  const { subtitle } = await getAuthenticatedShell(
    createSupabaseAuthRepository(),
    createSupabaseProfileRepository(),
  );

  const periods = await createSupabaseHistoryRepository().listPeriods();
  if (!periods) {
    redirect("/login");
  }

  const active = periods.find((period) => period.isActive) ?? null;
  const today = attendanceToday();
  const suggested = active ? suggestCloseDates(active.startDate, today) : null;
  const closed = periods.filter((period) => !period.isActive);

  return (
    <PeriodsShell subtitle={subtitle}>
      <h1 className="text-xl font-semibold">Nuevo corte</h1>
      {active ? (
        <>
          <p className="mt-2 text-sm text-puntual-secondary">
            Bloque activo: {active.title}
          </p>
          <p className="text-sm text-puntual-secondary">
            {formatWorkDate(active.startDate)} – {active.endDate ? formatWorkDate(active.endDate) : "actualidad"}
          </p>
          {suggested ? (
            <ClosePeriodForm
              key={active.id}
              defaultEnd={suggested.endDate}
              defaultStart={suggested.newStartDate}
            />
          ) : null}
        </>
      ) : (
        <p className="mt-3 text-sm text-puntual-secondary">No hay un periodo activo para cerrar.</p>
      )}
      {closed.length > 0 ? (
        <section className="mt-8">
          <h2 className="font-semibold">Bloques anteriores</h2>
          <ul className="mt-3 flex flex-col gap-2">
            {closed.map((period) => (
              <li key={period.id} className="text-sm text-puntual-secondary">{period.title}</li>
            ))}
          </ul>
        </section>
      ) : null}
    </PeriodsShell>
  );
}

function PeriodsShell({ subtitle, children }: { subtitle: string; children?: ReactNode }) {
  return (
    <main className="mx-auto flex min-h-full w-full max-w-md flex-col px-4 py-8">
      <AppHeader subtitle={subtitle} current="periodos" />
      <section className="mt-6 rounded-3xl bg-white px-6 py-8">{children}</section>
    </main>
  );
}
