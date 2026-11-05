"use client";

import { useActionState } from "react";

import { closePeriodAction, type ClosePeriodFormState } from "@/app/periodos/actions";

const initialState: ClosePeriodFormState = {
  errorMessage: null,
  successMessage: null,
};

export function ClosePeriodForm({
  defaultEnd,
  defaultStart,
}: {
  defaultEnd: string;
  defaultStart: string;
}) {
  const [state, formAction, pending] = useActionState(closePeriodAction, initialState);

  return (
    <form action={formAction} className="mt-6 flex flex-col gap-3">
      <p className="text-sm text-puntual-secondary">
        El bloque actual quedará en el historial. El nuevo empieza con conteo en cero.
      </p>
      <label className="flex flex-col gap-2 text-sm font-medium text-puntual-text">
        Último día del bloque
        <input
          name="endDate"
          type="date"
          defaultValue={defaultEnd}
          required
          disabled={pending}
          className="h-12 rounded-xl border border-puntual-muted bg-white px-3"
        />
      </label>
      <label className="flex flex-col gap-2 text-sm font-medium text-puntual-text">
        Primer día del nuevo bloque
        <input
          name="newStartDate"
          type="date"
          defaultValue={defaultStart}
          required
          disabled={pending}
          className="h-12 rounded-xl border border-puntual-muted bg-white px-3"
        />
      </label>
      <p className="text-sm text-puntual-secondary">
        El inicio debe ser después del cierre y no después de hoy.
      </p>
      <label className="flex flex-col gap-2 text-sm font-medium text-puntual-text">
        Nombre del nuevo bloque (opcional)
        <input
          name="newTitle"
          type="text"
          maxLength={80}
          disabled={pending}
          className="h-12 rounded-xl border border-puntual-muted bg-white px-3"
        />
      </label>
      {state.errorMessage ? (
        <p role="alert" className="text-sm text-puntual-error">{state.errorMessage}</p>
      ) : null}
      {state.successMessage ? (
        <p className="text-sm text-puntual-ontime">{state.successMessage}</p>
      ) : null}
      <button
        type="submit"
        disabled={pending}
        className="h-12 rounded-full bg-puntual-green font-semibold text-white disabled:opacity-60"
      >
        {pending ? "Cerrando..." : "Cerrar y abrir"}
      </button>
    </form>
  );
}
