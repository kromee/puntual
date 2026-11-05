import { connection } from "next/server";
import { redirect } from "next/navigation";
import { Suspense, type ReactNode } from "react";

import { getSession } from "@/application/auth/get-session";
import { formatWorkDate } from "@/application/attendance/format-entry";
import { absenceStatusLabel, absenceTypeLabel } from "@/domain/attendance/absence";
import { attendanceToday } from "@/domain/attendance/attendance-clock";
import { deleteAbsenceAction } from "@/app/ausencias/actions";
import { AbsenceForm } from "@/components/absences/absence-form";
import { AppHeader } from "@/components/shell/app-header";
import { createSupabaseAbsenceRepository } from "@/infrastructure/attendance/supabase-absence-repository";
import { createSupabaseAuthRepository } from "@/infrastructure/auth/supabase-auth-repository";
import { createSupabasePeriodRepository } from "@/infrastructure/attendance/supabase-period-repository";

export default function AbsencesPage() {
  return (
    <Suspense fallback={<AbsencesShell email="Comprobando tu acceso." />}>
      <AbsencesContent />
    </Suspense>
  );
}

async function AbsencesContent() {
  await connection();
  const session = await getSession(createSupabaseAuthRepository());
  if (!session) {
    redirect("/login");
  }

  const active = await createSupabasePeriodRepository().getActivePeriod();
  const today = attendanceToday();
  const absences = active
    ? await createSupabaseAbsenceRepository().listForPeriod(active.id)
    : [];

  return (
    <AbsencesShell email={session.email}>
      <h1 className="text-xl font-semibold">Registrar ausencia</h1>
      {active ? (
        <>
          <p className="mt-2 text-sm text-puntual-secondary">Bloque activo: {active.title}</p>
          <AbsenceForm today={today} />
        </>
      ) : (
        <p className="mt-3 text-sm text-puntual-secondary">
          No hay un bloque activo para registrar la ausencia.
        </p>
      )}
      {absences.length > 0 ? (
        <section className="mt-8">
          <h2 className="font-semibold">Ausencias del bloque</h2>
          <ul className="mt-3 flex flex-col gap-4">
            {absences.map((absence) => (
              <li key={absence.id} className="border-b border-puntual-background pb-3">
                <p className="font-medium">{absenceTypeLabel(absence.type)}</p>
                <p className="text-sm text-puntual-secondary">
                  {formatWorkDate(absence.startDate)} – {formatWorkDate(absence.endDate)}
                </p>
                <p className="text-sm text-puntual-secondary">
                  {absenceStatusLabel(absence.status)}
                  {absence.reason ? ` · ${absence.reason}` : ""}
                </p>
                <form action={deleteAbsenceAction} className="mt-2">
                  <input type="hidden" name="id" value={absence.id} />
                  <button type="submit" className="text-sm font-semibold text-puntual-error">
                    Quitar
                  </button>
                </form>
              </li>
            ))}
          </ul>
        </section>
      ) : null}
    </AbsencesShell>
  );
}

function AbsencesShell({ email, children }: { email: string; children?: ReactNode }) {
  return (
    <main className="mx-auto flex min-h-full w-full max-w-md flex-col px-4 py-8">
      <AppHeader email={email} current="ausencias" />
      <section className="mt-6 rounded-3xl bg-white px-6 py-8">{children}</section>
    </main>
  );
}
