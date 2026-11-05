import { signOutAction } from "@/app/auth/actions";

export function AppHeader({ subtitle }: { subtitle: string }) {
  return (
    <header className="shrink-0 bg-puntual-green px-5 pb-6 pt-[max(1.25rem,env(safe-area-inset-top))] text-white">
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="text-2xl font-bold tracking-tight">Puntual</p>
          <p className="truncate text-sm text-white/90">{subtitle}</p>
        </div>
        <form action={signOutAction}>
          <button
            type="submit"
            className="text-sm font-semibold text-white/90 underline-offset-2 hover:underline"
          >
            Salir
          </button>
        </form>
      </div>
    </header>
  );
}
