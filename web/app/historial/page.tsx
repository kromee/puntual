import { connection } from "next/server";
import Link from "next/link";
import { redirect } from "next/navigation";
import { Suspense, type ReactNode } from "react";

import { getSession } from "@/application/auth/get-session";
import { getHistory } from "@/application/history/get-history";
import { attendanceToday } from "@/domain/attendance/attendance-clock";
import { signOutAction } from "@/app/auth/actions";
import { createSupabaseAuthRepository } from "@/infrastructure/auth/supabase-auth-repository";
import { createSupabaseHistoryRepository } from "@/infrastructure/attendance/supabase-history-repository";

type HistorySearchParams = {
  period?: string | string[];
  year?: string | string[];
  month?: string | string[];
};

export default function HistoryPage({
  searchParams,
}: {
  searchParams: Promise<HistorySearchParams>;
}) {
  return (
    <Suspense fallback={<HistoryShell email="Comprobando tu acceso." />}>
      <HistoryContent searchParams={searchParams} />
    </Suspense>
  );
}

async function HistoryContent({
  searchParams,
}: {
  searchParams: Promise<HistorySearchParams>;
}) {
  await connection();
  const session = await getSession(createSupabaseAuthRepository());
  if (!session) {
    redirect("/login");
  }

  const params = await searchParams;
  const requested = {
    periodId: numberParam(params.period),
    year: numberParam(params.year),
    month: numberParam(params.month),
  };
  const history = await getHistory(createSupabaseHistoryRepository(), {
    today: attendanceToday(),
    ...requested,
  });

  if (!history) {
    redirect("/login");
  }

  const canonical = historyHref(history.periodId, history.year, history.month);
  if (
    one(params.period) !== String(history.periodId)
    || one(params.year) !== String(history.year)
    || one(params.month) !== String(history.month)
  ) {
    redirect(canonical);
  }

  return (
    <HistoryShell email={session.email}>
      <form action="/historial" className="flex flex-col gap-3">
        <label className="flex flex-col gap-1 text-sm font-medium text-puntual-text">
          Bloque
          <select
            name="period"
            defaultValue={String(history.periodId)}
            className="h-12 rounded-xl border border-puntual-muted bg-white px-3"
          >
            {history.periods.map((period) => (
              <option key={period.id} value={period.id}>
                {period.title}{period.isActive ? " · activo" : ""}
              </option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-1 text-sm font-medium text-puntual-text">
          Año
          <select
            name="year"
            defaultValue={String(history.year)}
            className="h-12 rounded-xl border border-puntual-muted bg-white px-3"
          >
            {history.years.map((year) => (
              <option key={year} value={year}>{year}</option>
            ))}
          </select>
        </label>
        <input type="hidden" name="month" value={history.month} />
        <button type="submit" className="h-11 rounded-full border border-puntual-green font-semibold text-puntual-green">
          Ver
        </button>
      </form>
      <p className="mt-3 text-sm text-puntual-secondary">{history.periodRange}</p>

      <div className="mt-6 flex items-center justify-between gap-3">
        {history.previous ? (
          <Link className="text-sm font-semibold text-puntual-green" href={historyHref(history.periodId, history.previous.year, history.previous.month)}>
            Anterior
          </Link>
        ) : <span className="text-sm text-puntual-muted">Anterior</span>}
        <h1 className="text-center text-lg font-semibold">{history.monthTitle}</h1>
        {history.next ? (
          <Link className="text-sm font-semibold text-puntual-green" href={historyHref(history.periodId, history.next.year, history.next.month)}>
            Siguiente
          </Link>
        ) : <span className="text-sm text-puntual-muted">Siguiente</span>}
      </div>

      <section className="mt-6 rounded-2xl bg-puntual-background p-4">
        <h2 className="font-semibold">Resumen del mes</h2>
        <p className="mt-2 text-sm">Este mes: {history.monthTotalLabel}</p>
        <p className="text-sm text-puntual-secondary">Total retardos: {history.monthLateDays}</p>
        <h2 className="mt-4 font-semibold">Acumulado del año</h2>
        <p className="mt-2 text-sm">{history.yearTotalLabel}</p>
        <p className="text-sm text-puntual-secondary">Total retardos: {history.yearLateDays}</p>
      </section>

      <section className="mt-6">
        <h2 className="font-semibold">Por mes</h2>
        <ul className="mt-3 flex flex-col gap-2">
          {history.months.map((month) => (
            <li key={month.label} className="flex items-center justify-between gap-3 text-sm">
              <span>{month.label}</span>
              <span className="text-puntual-secondary">
                {month.hasData ? `${month.lateDays} días con retardo · ${month.totalLabel}` : "—"}
              </span>
            </li>
          ))}
        </ul>
      </section>

      <section className="mt-6">
        <h2 className="font-semibold">Detalle día a día</h2>
        {history.isEmpty ? (
          <p className="mt-3 text-sm text-puntual-secondary">
            {history.isCurrentMonth
              ? "Sin registros este mes. Cuando hagas check-in, aparecerán aquí."
              : "Aún no hay registros en este mes."}
          </p>
        ) : (
          <ul className="mt-3 flex flex-col gap-3">
            {history.rows.map((row) => (
              <li key={row.key} className="flex items-start justify-between gap-3 border-b border-puntual-background pb-3">
                <div>
                  <p className="font-medium">{row.dayLabel}</p>
                  <p className="text-sm text-puntual-secondary">{row.timeLabel}</p>
                </div>
                <p className={`text-sm font-semibold ${toneClass(row.tone)}`}>{row.delayLabel}</p>
              </li>
            ))}
          </ul>
        )}
      </section>
    </HistoryShell>
  );
}

function HistoryShell({
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
          <Link href="/" className="text-sm font-semibold text-puntual-green">Entrada</Link>
          <form action={signOutAction}>
            <button type="submit" className="text-sm font-semibold text-puntual-secondary">Salir</button>
          </form>
        </div>
      </header>
      <section className="mt-6 rounded-3xl bg-white px-6 py-8">{children}</section>
    </main>
  );
}

function historyHref(periodId: number, year: number, month: number): string {
  return `/historial?period=${periodId}&year=${year}&month=${month}`;
}

function numberParam(value: string | string[] | undefined): number | null {
  const raw = one(value);
  if (!raw) {
    return null;
  }
  const parsed = Number(raw);
  return Number.isInteger(parsed) ? parsed : null;
}

function one(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

function toneClass(tone: "ontime" | "late" | "absence"): string {
  if (tone === "late") {
    return "text-puntual-late";
  }
  if (tone === "ontime") {
    return "text-puntual-ontime";
  }
  return "text-puntual-secondary";
}
