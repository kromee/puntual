import { redirect } from "next/navigation";
import { Suspense, type ReactNode } from "react";

import { getSession } from "@/application/auth/get-session";
import { signOutAction } from "@/app/auth/actions";
import { createSupabaseAuthRepository } from "@/infrastructure/auth/supabase-auth-repository";

export default function HomePage() {
  return (
    <Suspense fallback={<SessionShell title="Cargando sesión" detail="Comprobando tu acceso." />}>
      <AuthenticatedHome />
    </Suspense>
  );
}

async function AuthenticatedHome() {
  const session = await getSession(createSupabaseAuthRepository());
  if (!session) {
    redirect("/login");
  }

  return (
    <SessionShell title="Acceso confirmado" detail={session.email}>
      <form action={signOutAction} className="mt-6">
        <button
          type="submit"
          className="h-12 w-full rounded-xl border border-puntual-green font-semibold text-puntual-green"
        >
          Cerrar sesión
        </button>
      </form>
    </SessionShell>
  );
}

function SessionShell({
  title,
  detail,
  children,
}: {
  title: string;
  detail: string;
  children?: ReactNode;
}) {
  return (
    <main className="mx-auto flex min-h-full w-full max-w-md flex-col px-4 py-10">
      <p className="text-3xl font-bold text-puntual-green">Puntual</p>
      <p className="mt-1 text-lg text-puntual-secondary">Sesión iniciada</p>
      <section className="mt-6 rounded-2xl bg-white p-5">
        <h1 className="text-xl font-semibold text-puntual-text">{title}</h1>
        <p className="mt-2 text-puntual-secondary">{detail}</p>
        {children}
      </section>
    </main>
  );
}
