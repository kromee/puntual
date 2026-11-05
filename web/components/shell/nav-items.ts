export const appNavItems = [
  { href: "/", label: "Entrada", id: "entrada" },
  { href: "/historial", label: "Historial", id: "historial" },
  { href: "/periodos", label: "Periodos", id: "periodos" },
  { href: "/ausencias", label: "Ausencias", id: "ausencias" },
  { href: "/perfil", label: "Perfil", id: "perfil" },
] as const;

export type AppNavId = (typeof appNavItems)[number]["id"];
