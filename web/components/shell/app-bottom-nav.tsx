import Link from "next/link";

import { appNavItems, type AppNavId } from "@/components/shell/nav-items";

function NavIcon({ id, active }: { id: AppNavId; active: boolean }) {
  const className = active ? "text-puntual-green" : "text-puntual-muted";
  switch (id) {
    case "entrada":
      return (
        <svg aria-hidden className={`size-6 ${className}`} viewBox="0 0 24 24" fill="currentColor">
          <path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67V7z" />
        </svg>
      );
    case "historial":
      return (
        <svg aria-hidden className={`size-6 ${className}`} viewBox="0 0 24 24" fill="currentColor">
          <path d="M13 3a9 9 0 00-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7v2c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z" />
        </svg>
      );
    case "periodos":
      return (
        <svg aria-hidden className={`size-6 ${className}`} viewBox="0 0 24 24" fill="currentColor">
          <path d="M19 4h-1V2h-2v2H8V2H6v2H5a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2V6a2 2 0 00-2-2zm0 16H5V10h14v10zM5 8V6h14v2H5z" />
        </svg>
      );
    case "ausencias":
      return (
        <svg aria-hidden className={`size-6 ${className}`} viewBox="0 0 24 24" fill="currentColor">
          <path d="M19 4h-1V2h-2v2H8V2H6v2H5a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2V6a2 2 0 00-2-2zm0 16H5V10h14v10zm-7-2h2v-2h-2v2zm0-4h2v-2h-2v2zm0-4h2V8h-2v2z" />
        </svg>
      );
    case "perfil":
      return (
        <svg aria-hidden className={`size-6 ${className}`} viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
        </svg>
      );
  }
}

export function AppBottomNav({ current }: { current: AppNavId }) {
  return (
    <nav
      aria-label="Secciones principales"
      className="shrink-0 border-t border-puntual-background bg-white pt-1 pb-[max(0.5rem,env(safe-area-inset-bottom))]"
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
