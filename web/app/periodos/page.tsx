import { connection } from "next/server";
import { redirect } from "next/navigation";
import { Suspense, type ReactNode } from "react";

import { getSession } from "@/application/auth/get-session";
import { formatWorkDate } from "@/application/attendance/format-entry";
import { attendanceToday } from "@/domain/attendance/attendance-clock";
import { suggestCloseDates } from "@/domain/attendance/close-period";
import { ClosePeriodForm } from "@/components/periods/close-period-form";
import { AppHeader } from "@/components/shell/app-header";
import { createSupabaseAuthRepository } from "@/infrastructure/auth/supabase-auth-repository";
import { createSupabaseHistoryRepository } from "@/infrastructure/attendance/supabase-history-repository";

export default function PeriodsPage() {
  return (
    <Suspense fallback={<PeriodsShell email="Comprobando tu acceso." />}>
      <PeriodsContent />
    </Suspense>
  );
}

async function PeriodsContent() {
  await connection();
  const session = await getSession(createSupabaseAuthRepository());
  if (!session) {
    redirect("/login");
  }

  const periods = await createSupabaseHistoryRepository().listPeriods();
  if (!periods) {
    redirect("/login");
  }

  const active = periods.find((period) => period.isActive) ?? null;
  const today = attendanceToday();
  const suggested = active ? suggestCloseDates(active.startDate, today) : null;
  const closed = periods.filter((period) => !period.isActive);

  return (
    <PeriodsShell email={session.email}>
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

function PeriodsShell({ email, children }: { email: string; children?: ReactNode }) {
  return (
    <main className="mx-auto flex min-h-full w-full max-w-md flex-col px-4 py-8">
      <AppHeader email={email} current="periodos" />
      <section className="mt-6 rounded-3xl bg-white px-6 py-8">{children}</section>
    </main>
  );
}
