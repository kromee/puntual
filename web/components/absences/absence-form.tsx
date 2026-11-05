"use client";

import { useActionState } from "react";

import { saveAbsenceAction, type SaveAbsenceFormState } from "@/app/ausencias/actions";
import { ABSENCE_TYPES, absenceTypeLabel } from "@/domain/attendance/absence";

const initialState: SaveAbsenceFormState = {
  errorMessage: null,
  successMessage: null,
};

export function AbsenceForm({ today }: { today: string }) {
  const [state, formAction, pending] = useActionState(saveAbsenceAction, initialState);

  return (
    <form action={formAction} className="mt-6 flex flex-col gap-3">
      <label className="flex flex-col gap-2 text-sm font-medium text-puntual-text">
        Desde
        <input
          name="startDate"
          type="date"
          defaultValue={today}
          required
          disabled={pending}
          className="h-12 rounded-xl border border-puntual-muted bg-white px-3"
        />
      </label>
      <label className="flex flex-col gap-2 text-sm font-medium text-puntual-text">
        Hasta
        <input
          name="endDate"
          type="date"
          defaultValue={today}
          required
          disabled={pending}
          className="h-12 rounded-xl border border-puntual-muted bg-white px-3"
        />
      </label>
      <label className="flex flex-col gap-2 text-sm font-medium text-puntual-text">
        Tipo
        <select
          name="type"
          defaultValue="VACATION"
          disabled={pending}
          className="h-12 rounded-xl border border-puntual-muted bg-white px-3"
        >
          {ABSENCE_TYPES.map((type) => (
            <option key={type} value={type}>{absenceTypeLabel(type)}</option>
          ))}
        </select>
      </label>
      <label className="flex flex-col gap-2 text-sm font-medium text-puntual-text">
        Motivo
        <input
          name="reason"
          type="text"
          maxLength={200}
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
        {pending ? "Guardando..." : "Registrar ausencia"}
      </button>
    </form>
  );
}
