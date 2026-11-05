import Link from "next/link";

import { signOutAction } from "@/app/auth/actions";

const links = [
  { href: "/", label: "Entrada", id: "entrada" },
  { href: "/historial", label: "Historial", id: "historial" },
  { href: "/periodos", label: "Periodos", id: "periodos" },
  { href: "/ausencias", label: "Ausencias", id: "ausencias" },
] as const;

export function AppHeader({
  email,
  current,
}: {
  email: string;
  current: (typeof links)[number]["id"];
}) {
  return (
    <header>
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-3xl font-bold text-puntual-green">Puntual</p>
          <p className="text-sm text-puntual-secondary">{email}</p>
        </div>
        <form action={signOutAction}>
          <button type="submit" className="text-sm font-semibold text-puntual-secondary">
            Salir
          </button>
        </form>
      </div>
      <nav className="mt-4 flex flex-wrap gap-x-4 gap-y-2 text-sm">
        {links.map((link) => (
          <Link
            key={link.id}
            href={link.href}
            className={link.id === current ? "font-semibold text-puntual-green" : "text-puntual-secondary"}
          >
            {link.label}
          </Link>
        ))}
      </nav>
    </header>
  );
}
