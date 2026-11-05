import type { ReactNode } from "react";

import { signOutAction } from "@/app/auth/actions";
import { AppBottomNav } from "@/components/shell/app-bottom-nav";
import { AppHeader } from "@/components/shell/app-header";
import { AppSideNav } from "@/components/shell/app-side-nav";
import type { AppNavId } from "@/components/shell/nav-items";

export function AppShell({
  subtitle,
  current,
  children,
  contentClassName = "",
}: {
  subtitle: string;
  current: AppNavId;
  children?: ReactNode;
  contentClassName?: string;
}) {
  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-md flex-col bg-puntual-background lg:max-w-6xl lg:px-8 lg:py-8">
      <div className="flex min-h-dvh flex-1 flex-col lg:min-h-0 lg:flex-row lg:gap-6">
        <aside className="hidden lg:flex lg:w-60 lg:shrink-0 lg:flex-col lg:rounded-3xl lg:bg-white lg:px-5 lg:py-6 lg:shadow-sm">
          <div className="mb-8 min-w-0">
            <p className="text-2xl font-bold text-puntual-green">Puntual</p>
            <p className="mt-1 truncate text-sm text-puntual-secondary">{subtitle}</p>
          </div>
          <AppSideNav current={current} />
          <form action={signOutAction} className="mt-auto pt-6">
            <button
              type="submit"
              className="w-full rounded-xl border border-puntual-muted px-3 py-2.5 text-sm font-semibold text-puntual-secondary hover:bg-puntual-background"
            >
              Salir
            </button>
          </form>
        </aside>

        <div className="flex min-h-0 flex-1 flex-col lg:min-h-[calc(100dvh-4rem)]">
          <div className="lg:hidden">
            <AppHeader subtitle={subtitle} />
          </div>
          <div className="-mt-4 flex min-h-0 flex-1 flex-col overflow-y-auto px-4 pb-3 pt-0 lg:mt-0 lg:px-0 lg:pb-0">
            <section
              className={`flex flex-col rounded-3xl bg-white px-6 py-8 shadow-sm lg:min-h-full lg:px-10 lg:py-10 ${contentClassName}`}
            >
              {children}
            </section>
          </div>
          <AppBottomNav current={current} />
        </div>
      </div>
    </main>
  );
}
