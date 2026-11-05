import type { ReactNode } from "react";

import { AppBottomNav } from "@/components/shell/app-bottom-nav";
import { AppHeader } from "@/components/shell/app-header";
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
    <main className="mx-auto flex min-h-dvh w-full max-w-md flex-col bg-puntual-background">
      <AppHeader subtitle={subtitle} />
      <div className="-mt-4 flex min-h-0 flex-1 flex-col overflow-y-auto px-4 pb-3 pt-0">
        <section
          className={`flex flex-col rounded-3xl bg-white px-6 py-8 shadow-sm ${contentClassName}`}
        >
          {children}
        </section>
      </div>
      <AppBottomNav current={current} />
    </main>
  );
}
