"use client";

import { useActionState } from "react";

import {
  registerManualCheckInAction,
  type ManualCheckInFormState,
} from "@/app/historial/actions";

const initialState: ManualCheckInFormState = {
  errorMessage: null,
  successMessage: null,
};

export function ManualCheckInForm({
  periodId,
  defaultWorkDate,
}: {
  periodId: number;
  defaultWorkDate: string;
}) {
  const [state, formAction, pending] = useActionState(registerManualCheckInAction, initialState);

  return (
    <form action={formAction} className="mt-3 flex flex-col gap-3">
      <input type="hidden" name="periodId" value={periodId} />
      <label className="flex flex-col gap-2 text-sm font-medium text-puntual-text">
        Fecha
        <input
          name="workDate"
          type="date"
          defaultValue={defaultWorkDate}
          required
          disabled={pending}
          className="h-12 rounded-xl border border-puntual-muted bg-white px-3"
        />
      </label>
      <label className="flex flex-col gap-2 text-sm font-medium text-puntual-text">
        Hora de llegada
        <input
          name="time"
          type="time"
          defaultValue="09:00"
          required
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
        className="h-11 rounded-full border border-puntual-green font-semibold text-puntual-green disabled:opacity-60"
      >
        {pending ? "Guardando..." : "Agregar registro manual"}
      </button>
    </form>
  );
}
