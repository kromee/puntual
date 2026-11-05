import Link from "next/link";
import { redirect } from "next/navigation";
import { Suspense, type ReactNode } from "react";

import { getSession } from "@/application/auth/get-session";
import { formatCheckInTime, formatDelay, formatWorkDate } from "@/application/attendance/format-entry";
import { getTodayEntry } from "@/application/attendance/get-today-entry";
import { signOutAction } from "@/app/auth/actions";
import { CorrectForm } from "@/components/check-in/correct-form";
import { RegisterForm } from "@/components/check-in/register-form";
import { createSupabaseCheckInRepository } from "@/infrastructure/attendance/supabase-check-in-repository";
import { createSupabaseAuthRepository } from "@/infrastructure/auth/supabase-auth-repository";

export default function HomePage() {
  return (
    <Suspense fallback={<EntryShell email="Comprobando tu acceso." />}>
      <EntryHome />
    </Suspense>
  );
}

async function EntryHome() {
  const session = await getSession(createSupabaseAuthRepository());
  if (!session) {
    redirect("/login");
  }

  const entry = await getTodayEntry(createSupabaseCheckInRepository());
  if (!entry) {
    redirect("/login");
  }

  const status = entryStatus(entry);

  return (
    <EntryShell email={session.email}>
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
    </EntryShell>
  );
}

function EntryShell({
  email,
  children,
}: {
  email: string;
  children?: ReactNode;
}) {
  return (
    <main className="mx-auto flex min-h-full w-full max-w-md flex-col px-4 py-8">
      <header className="flex items-start justify-between gap-4">
        <div>
          <p className="text-3xl font-bold text-puntual-green">Puntual</p>
          <p className="text-sm text-puntual-secondary">{email}</p>
        </div>
        <div className="flex items-center gap-3">
          <Link href="/historial" className="text-sm font-semibold text-puntual-green">
            Historial
          </Link>
          <form action={signOutAction}>
            <button type="submit" className="text-sm font-semibold text-puntual-secondary">
              Salir
            </button>
          </form>
        </div>
      </header>
      <section className="mt-6 flex flex-1 flex-col rounded-3xl bg-white px-6 py-8">{children}</section>
    </main>
  );
}

function entryStatus(entry: {
  isWorkday: boolean;
  outsidePeriod: boolean;
  checkIn: { checkedInAt: string; delayMinutes: number } | null;
}) {
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
      detail: "Haz un corte cuando exista el módulo de periodos.",
      detailClassName: "text-puntual-secondary",
      action: "none" as const,
    };
  }
  if (!entry.checkIn) {
    return {
      subtitle: "Sin hora configurada · solo registro",
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
