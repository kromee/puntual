import { redirect } from "next/navigation";
import { Suspense } from "react";

import { getAuthenticatedShell } from "@/application/shell/get-authenticated-shell";
import { formatWorkDate } from "@/application/attendance/format-entry";
import { attendanceToday } from "@/domain/attendance/attendance-clock";
import { suggestCloseDates } from "@/domain/attendance/close-period";
import { ClosePeriodForm } from "@/components/periods/close-period-form";
import { AppShell } from "@/components/shell/app-shell";
import { createSupabaseAuthRepository } from "@/infrastructure/auth/supabase-auth-repository";
import { createSupabaseHistoryRepository } from "@/infrastructure/attendance/supabase-history-repository";
import { createSupabaseProfileRepository } from "@/infrastructure/profile/supabase-profile-repository";

export default function PeriodsPage() {
  return (
    <Suspense fallback={<AppShell subtitle="Comprobando tu acceso." current="periodos" />}>
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
    <AppShell subtitle={subtitle} current="periodos">
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
    </AppShell>
  );
}
