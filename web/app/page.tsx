import { redirect } from "next/navigation";
import { Suspense } from "react";

import { formatCheckInTime, formatDelay, formatWorkDate } from "@/application/attendance/format-entry";
import { formatExpectedTime } from "@/application/profile/format-expected-time";
import { getTodayEntry } from "@/application/attendance/get-today-entry";
import { getAuthenticatedShell } from "@/application/shell/get-authenticated-shell";
import { CorrectForm } from "@/components/check-in/correct-form";
import { RegisterForm } from "@/components/check-in/register-form";
import { AppShell } from "@/components/shell/app-shell";
import { createSupabaseCheckInRepository } from "@/infrastructure/attendance/supabase-check-in-repository";
import { createSupabaseAuthRepository } from "@/infrastructure/auth/supabase-auth-repository";
import { createSupabaseProfileRepository } from "@/infrastructure/profile/supabase-profile-repository";

export default function HomePage() {
  return (
    <Suspense fallback={<AppShell subtitle="Comprobando tu acceso." current="entrada" />}>
      <EntryHome />
    </Suspense>
  );
}

async function EntryHome() {
  const { profile, subtitle } = await getAuthenticatedShell(
    createSupabaseAuthRepository(),
    createSupabaseProfileRepository(),
  );

  const entry = await getTodayEntry(createSupabaseCheckInRepository());
  if (!entry) {
    redirect("/login");
  }

  const status = entryStatus(entry, profile.hasExpectedTime, profile);

  return (
    <AppShell subtitle={subtitle} current="entrada">
      <p className="text-sm text-puntual-secondary">{entry.period.title}</p>
      <p className="mt-6 text-4xl text-puntual-muted">{formatWorkDate(entry.workDate)}</p>
      <p className="mt-2 text-sm text-puntual-secondary">{status.subtitle}</p>
      <div className="mt-10 text-center">
        <h1 className="text-2xl font-bold text-puntual-text">{status.title}</h1>
        {status.detail ? (
          <p className={`mt-2 text-lg font-semibold ${status.detailClassName}`}>{status.detail}</p>
        ) : null}
      </div>
      {status.action === "register" ? <RegisterForm /> : null}
      {status.action === "correct" && entry.checkIn ? (
        <CorrectForm defaultTime={formatCheckInTime(entry.checkIn.checkedInAt)} />
      ) : null}
    </AppShell>
  );
}

function entryStatus(
  entry: {
    isWorkday: boolean;
    outsidePeriod: boolean;
    checkIn: { checkedInAt: string; delayMinutes: number } | null;
  },
  hasExpectedTime: boolean,
  profile: { expectedHour: number; expectedMinute: number },
) {
  if (!entry.isWorkday) {
    return {
      subtitle: "Lunes a viernes",
      title: "Hoy no es día laborable",
      detail: "El registro es de lunes a viernes.",
      detailClassName: "text-puntual-secondary",
      action: "none" as const,
    };
  }
  if (entry.outsidePeriod) {
    return {
      subtitle: "Bloque activo",
      title: "Hoy queda fuera del bloque",
      detail: "Haz un corte en Periodos si necesitas ampliar el bloque.",
      detailClassName: "text-puntual-secondary",
      action: "none" as const,
    };
  }
  if (!entry.checkIn) {
    return {
      subtitle: hasExpectedTime
        ? `Hora esperada · ${formatExpectedTime(profile.expectedHour, profile.expectedMinute)}`
        : "Sin hora configurada · solo registro",
      title: "Presiona el botón al llegar",
      detail: null,
      detailClassName: "",
      action: "register" as const,
    };
  }
  const onTime = entry.checkIn.delayMinutes <= 0;
  return {
    subtitle: "Entrada registrada",
    title: formatCheckInTime(entry.checkIn.checkedInAt),
    detail: formatDelay(entry.checkIn.delayMinutes),
    detailClassName: onTime ? "text-puntual-ontime" : "text-puntual-late",
    action: "correct" as const,
  };
}
