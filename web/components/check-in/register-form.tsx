"use client";

import { useActionState } from "react";

import { registerCheckInAction, type EntryFormState } from "@/app/entry/actions";

const initialState: EntryFormState = { errorMessage: null };

export function RegisterForm() {
  const [state, formAction, pending] = useActionState(registerCheckInAction, initialState);

  return (
    <form action={formAction} className="mt-6">
      {state.errorMessage ? (
        <p role="alert" className="mb-3 text-sm text-puntual-error">
          {state.errorMessage}
        </p>
      ) : null}
      <button
        type="submit"
        disabled={pending}
        className="h-12 w-full rounded-full bg-puntual-green text-base font-semibold text-white disabled:opacity-60"
      >
        {pending ? "Registrando..." : "Registrar entrada"}
      </button>
    </form>
  );
}
