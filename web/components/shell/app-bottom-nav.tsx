import Link from "next/link";

import { NavIcon } from "@/components/shell/nav-icon";
import { appNavItems, type AppNavId } from "@/components/shell/nav-items";

export function AppBottomNav({ current }: { current: AppNavId }) {
  return (
    <nav
      aria-label="Secciones principales"
      className="shrink-0 border-t border-puntual-background bg-white pt-1 pb-[max(0.5rem,env(safe-area-inset-bottom))] lg:hidden"
    >
      <ul className="grid grid-cols-5 px-1">
        {appNavItems.map((item) => {
          const active = item.id === current;
          return (
            <li key={item.id}>
              <Link
                href={item.href}
                className={`flex flex-col items-center gap-0.5 rounded-xl px-1 py-2 text-[10px] leading-tight font-medium sm:text-xs ${
                  active ? "text-puntual-green" : "text-puntual-muted"
                }`}
              >
                <span
                  className={`flex size-9 items-center justify-center rounded-2xl ${
                    active ? "bg-puntual-green/10" : ""
                  }`}
                >
                  <NavIcon id={item.id} active={active} />
                </span>
                {item.label}
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
