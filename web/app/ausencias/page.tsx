import { Suspense } from "react";

import { getAuthenticatedShell } from "@/application/shell/get-authenticated-shell";
import { formatWorkDate } from "@/application/attendance/format-entry";
import { absenceStatusLabel, absenceTypeLabel } from "@/domain/attendance/absence";
import { attendanceToday } from "@/domain/attendance/attendance-clock";
import { deleteAbsenceAction } from "@/app/ausencias/actions";
import { AbsenceForm } from "@/components/absences/absence-form";
import { AppShell } from "@/components/shell/app-shell";
import { createSupabaseAbsenceRepository } from "@/infrastructure/attendance/supabase-absence-repository";
import { createSupabaseAuthRepository } from "@/infrastructure/auth/supabase-auth-repository";
import { createSupabasePeriodRepository } from "@/infrastructure/attendance/supabase-period-repository";
import { createSupabaseProfileRepository } from "@/infrastructure/profile/supabase-profile-repository";

const ABSENCE_TYPE_OPTIONS = [
  { value: "VACATION", label: "Vacaciones" },
  { value: "PERSONAL_PERMISSION", label: "Permiso" },
  { value: "SICK_LEAVE", label: "Incapacidad" },
  { value: "HOLIDAY", label: "Día festivo" },
  { value: "FIELD_WORK", label: "Trabajo fuera de oficina" },
];

export default function AbsencesPage() {
  return (
    <Suspense fallback={<AppShell subtitle="Comprobando tu acceso." current="ausencias" />}>
      <AbsencesContent />
    </Suspense>
  );
}

async function AbsencesContent() {
  const { subtitle } = await getAuthenticatedShell(
    createSupabaseAuthRepository(),
    createSupabaseProfileRepository(),
  );

  const active = await createSupabasePeriodRepository().getActivePeriod();
  const today = attendanceToday();
  const absences = active
    ? await createSupabaseAbsenceRepository().listForPeriod(active.id)
    : [];

  return (
    <AppShell subtitle={subtitle} current="ausencias">
      <h1 className="text-xl font-semibold">Registrar ausencia</h1>
      {active ? (
        <>
          <p className="mt-2 text-sm text-puntual-secondary">Bloque activo: {active.title}</p>
          <AbsenceForm
            today={today}
            types={ABSENCE_TYPE_OPTIONS}
          />
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
    </AppShell>
  );
}
