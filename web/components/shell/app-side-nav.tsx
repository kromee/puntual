import Link from "next/link";

import { NavIcon } from "@/components/shell/nav-icon";
import { appNavItems, type AppNavId } from "@/components/shell/nav-items";

export function AppSideNav({ current }: { current: AppNavId }) {
  return (
    <nav aria-label="Secciones principales">
      <ul className="flex flex-col gap-1">
        {appNavItems.map((item) => {
          const active = item.id === current;
          return (
            <li key={item.id}>
              <Link
                href={item.href}
                className={`flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors ${
                  active
                    ? "bg-puntual-green/10 text-puntual-green"
                    : "text-puntual-secondary hover:bg-puntual-background"
                }`}
              >
                <NavIcon id={item.id} active={active} />
                {item.label}
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
