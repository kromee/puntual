"use client";

import { useActionState } from "react";

import { correctCheckInAction, type EntryFormState } from "@/app/entry/actions";

const initialState: EntryFormState = { errorMessage: null };

export function CorrectForm({ defaultTime }: { defaultTime: string }) {
  const [state, formAction, pending] = useActionState(correctCheckInAction, initialState);

  return (
    <form action={formAction} className="mt-6 flex flex-col gap-3">
      <label className="flex flex-col gap-2 text-sm font-medium text-puntual-text">
        Hora real de llegada
        <input
          name="time"
          type="time"
          defaultValue={defaultTime}
          required
          disabled={pending}
          className="h-12 rounded-xl border border-puntual-muted bg-white px-3 text-base font-normal outline-none focus:border-puntual-green"
        />
      </label>
      <label className="flex flex-col gap-2 text-sm font-medium text-puntual-text">
        Motivo
        <input
          name="reason"
          type="text"
          minLength={3}
          maxLength={200}
          required
          disabled={pending}
          placeholder="Por qué se corrige"
          className="h-12 rounded-xl border border-puntual-muted bg-white px-3 text-base font-normal outline-none focus:border-puntual-green"
        />
      </label>
      {state.errorMessage ? (
        <p role="alert" className="text-sm text-puntual-error">
          {state.errorMessage}
        </p>
      ) : null}
      <button
        type="submit"
        disabled={pending}
        className="h-12 rounded-full border border-puntual-green font-semibold text-puntual-green disabled:opacity-60"
      >
        {pending ? "Guardando..." : "Corregir hora real"}
      </button>
    </form>
  );
}
