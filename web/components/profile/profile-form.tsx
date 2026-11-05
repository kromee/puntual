"use client";

import { useActionState } from "react";

import { saveProfileAction, type SaveProfileFormState } from "@/app/perfil/actions";
import { clockInputValue, formatExpectedTime } from "@/application/profile/format-expected-time";

const initialState: SaveProfileFormState = {
  errorMessage: null,
  successMessage: null,
};

export function ProfileForm({
  displayName,
  hasExpectedTime,
  expectedHour,
  expectedMinute,
}: {
  displayName: string;
  hasExpectedTime: boolean;
  expectedHour: number;
  expectedMinute: number;
}) {
  const [state, formAction, pending] = useActionState(saveProfileAction, initialState);

  return (
    <form action={formAction} className="mt-6 flex flex-col gap-4">
      <label className="flex flex-col gap-2 text-sm font-medium text-puntual-text">
        Tu nombre
        <input
          name="displayName"
          type="text"
          defaultValue={displayName}
          maxLength={80}
          disabled={pending}
          className="h-12 rounded-xl border border-puntual-muted bg-white px-3"
        />
      </label>
      <label className="flex items-center gap-3 text-sm font-medium text-puntual-text">
        <input
          name="useExpectedTime"
          type="checkbox"
          defaultChecked={hasExpectedTime}
          disabled={pending}
          className="size-4 accent-puntual-green"
        />
        Usar hora esperada para calcular retardo
      </label>
      <label className="flex flex-col gap-2 text-sm font-medium text-puntual-text">
        Hora esperada
        <input
          name="expectedTime"
          type="time"
          defaultValue={clockInputValue(expectedHour, expectedMinute)}
          disabled={pending}
          className="h-12 rounded-xl border border-puntual-muted bg-white px-3"
        />
      </label>
      <p className="text-sm text-puntual-secondary">
        {hasExpectedTime
          ? `Actualmente: ${formatExpectedTime(expectedHour, expectedMinute)}`
          : "Sin hora configurada: el registro web solo guarda evidencia y retardo 0."}
      </p>
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
        {pending ? "Guardando..." : "Guardar perfil"}
      </button>
    </form>
  );
}
